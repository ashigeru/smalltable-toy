/*
 * Copyright 2010 @ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.ashigeru.lab.smalltable.local;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ashigeru.lab.smalltable.Entity;
import com.ashigeru.lab.smalltable.Revision;
import com.ashigeru.lab.smalltable.Session;
import com.ashigeru.lab.smalltable.Entity.Reference;

/**
 * ローカルで完結する{@code smalltable}での{@link Session}の実装。
 * <p>
 * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class LocalSession implements Session {

    /**
     * このセッションを開始したリポジトリ。
     */
    private LocalRepository repository;

    /**
     * このセッションを開始したリビジョン。
     */
    private Revision<LocalEntityId> start;

    /**
     * 名前つき参照の変更一覧。
     */
    private Map<String, Entity.Reference> modifiedBindings;

    /**
     * インスタンスを生成する。
     * @param repository このセッションを開始したリポジトリ
     * @param start このセッションを開始したリビジョン
     */
    public LocalSession(LocalRepository repository, Revision<LocalEntityId> start) {
        if (repository == null) {
            throw new IllegalArgumentException("repository is null"); //$NON-NLS-1$
        }
        if (start == null) {
            throw new IllegalArgumentException("start is null"); //$NON-NLS-1$
        }
        this.repository = repository;
        this.start = start;
        this.modifiedBindings = new HashMap<String, Entity.Reference>();
    }

    @Override
    public Entity.Reference allocateReference() {
        return repository.getNextReference();
    }

    @Override
    public void bind(String name, Entity.Reference reference) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        modifiedBindings.put(name, reference);
    }

    @Override
    public Entity.Reference getBound(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        if (modifiedBindings.containsKey(name)) {
            return modifiedBindings.get(name);
        }
        return start.getBinding(name);
    }

    @Override
    public Entity resolve(Entity.Reference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("reference is null"); //$NON-NLS-1$
        }

        // リビジョン情報から、最新のエンティティに対応するIDを取得
        LocalEntityId id = start.getId(reference);
        if (id == null) {
            return null;
        }

        // リポジトリからエンティティの情報を取得
        return repository.getEntity(id);
    }

    @Override
    public void save(Collection<? extends Entity> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities is null"); //$NON-NLS-1$
        }

        // 開始リビジョンからの差分を計算する
        Map<String, Entity.Reference> bindingDelta = buildBindingDelta();

        // 余計なごみを最小にするため、事前検査を行う
        boolean verified = preverify(bindingDelta, entities);
        if (verified == false) {
            // FIXME 通知方法について考える
            throw new ConcurrentModificationException();
        }

        // 作成または更新されたエンティティの一覧を登録する (分割して実行してもよい)
        Map<Entity.Reference, LocalEntityId> entityDelta = repository.prepare(entities);

        // 変更差分を作成
        Revision.Delta<LocalEntityId> delta = new Revision.Delta<LocalEntityId>(bindingDelta, entityDelta);

        // 開始リビジョンからの変更差分をコミット
        Revision<LocalEntityId> next = repository.commit(start, delta);
        if (next == null) {
            // FIXME 通知方法について考える
            throw new ConcurrentModificationException();
        }

        // MEMO nextを開始リビジョンとし、ローカルのすべてのデータを同期させれば継続して利用できそう
    }

    private boolean preverify(Map<String, Reference> bindingDelta, Collection<? extends Entity> entities) {
        assert bindingDelta != null;
        assert entities != null;
        // TODO エンティティがコンパクションによって削除される場合、削除と削除の衝突を許す

        // FIXME 常に最新のリビジョンを利用する。ブランチする場合には別途考える
        Revision<LocalEntityId> head = repository.getHeadRevision();

        // 開始リビジョンから、「現在の」最新リビジョンまでの差分を作成
        Revision.Delta<LocalEntityId> delta = start.createDeltaTo(head);
        return delta.conflictsWith(bindingDelta.keySet(), buildEntityDelta(entities)) == false;
    }

    private Map<String, Entity.Reference> buildBindingDelta() {
        // 名前つき参照の変更一覧のうち、実質的な変更がないものを削除する
        Iterator<Map.Entry<String, Entity.Reference>> iter = modifiedBindings.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Entity.Reference> entry = iter.next();
            String name = entry.getKey();
            Entity.Reference value = entry.getValue();
            if (value == null) {
                if (start.getBinding(name) == null) {
                    iter.remove();
                }
            }
            else if (value.equals(start.getBinding(name))) {
                iter.remove();
            }
        }
        return new HashMap<String, Entity.Reference>(modifiedBindings);
    }

    private Set<Entity.Reference> buildEntityDelta(Collection<? extends Entity> entities) {
        assert entities != null;

        // TODO 削除されたものについても探せるといい
        Set<Entity.Reference> results = new HashSet<Entity.Reference>();
        for (Entity entity : entities) {
            results.add(entity.getSelfReference());
        }
        return results;
    }
}
