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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ashigeru.lab.smalltable.Entity;
import com.ashigeru.lab.smalltable.Revision;

/**
 * {@code smalltable}で利用するリビジョンの情報や、エンティティのすべての情報が格納されたリポジトリ。
 * <p>
 * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class LocalRepository implements Serializable {

    private static final long serialVersionUID = 942972032864289607L;

    /**
     * {@link #commit(Revision, Revision.Delta)}の再試行回数。
     */
    private static final int MAX_RETRY = 5;

    /**
     * リビジョンの一覧。
     * <p>
     * 末尾に行くほど古く、常に先頭に最新のリビジョン情報が格納されている。
     * </p>
     */
    private LinkedList<Revision<LocalEntityId>> revisions;

    /**
     * すべてのリビジョンのすべてのエンティティ。
     */
    private Map<LocalEntityId, Entity> allEntities;

    /**
     * {@link Entity.Reference}のための一意の番号を生成するシーケンス。
     */
    private AtomicLong referenceSequence;

    /**
     * {@link LocalEntityId}のための一意の番号を生成するシーケンス。
     */
    private AtomicLong entityIdSequence;

    /**
     * インスタンスを生成する。
     */
    public LocalRepository() {
        this.revisions = new LinkedList<Revision<LocalEntityId>>();
        this.allEntities = new HashMap<LocalEntityId, Entity>();
        this.referenceSequence = new AtomicLong();
        this.entityIdSequence = new AtomicLong();

        // 最初のリビジョンを作成して追加
        Revision<LocalEntityId> initial = new Revision<LocalEntityId>(
                Collections.<String, Entity.Reference>emptyMap(),
                Collections.<Entity.Reference, LocalEntityId>emptyMap());
        this.revisions.addFirst(initial);
    }

    /**
     * このリポジトリ上に最後にコミットされたリビジョンに対する、新しいセッションを作成して返す。
     * @return 作成したセッション
     */
    public LocalSession createSession() {
        assert revisions.isEmpty() == false;

        // FIXME 今回は常に最新のものを使う。実際には開始リビジョンを選べるとよさそう
        Revision<LocalEntityId> head = getHeadRevision();
        return new LocalSession(this, head);
    }

    /**
     * このリポジトリ上に最後にコミットされたリビジョンの情報を返す。
     * @return 最新のリビジョン
     */
    public Revision<LocalEntityId> getHeadRevision() {
        synchronized (revisions) {
            return revisions.getFirst();
        }
    }

    /**
     * このリポジトリ上で、まだ利用されていない新しい参照を作成して返す。
     * @return このリポジトリ上で、まだ利用されていない新しい参照
     */
    public Entity.Reference getNextReference() {
        return new Entity.Reference(referenceSequence.incrementAndGet());
    }

    /**
     * このリポジトリに格納された、指定の識別子をもつエンティティを返す。
     * @param id 対象の識別子
     * @return 対応するエンティティ、存在しない場合は{@code null}
     */
    public Entity getEntity(LocalEntityId id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null"); //$NON-NLS-1$
        }
        return allEntities.get(id);
    }

    /**
     * 指定のエンティティ一覧をこのリポジトリ上に追加し、それぞれの識別子を返す。
     * @param entities 追加するエンティティの一覧
     * @return 追加したエンティティへの参照と、その識別子の一覧
     * @see #getEntity(LocalEntityId)
     */
    public Map<Entity.Reference, LocalEntityId> prepare(Collection<? extends Entity> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities is null"); //$NON-NLS-1$
        }

        // エンティティの個数分だけIDを確保
        long lastSequence = entityIdSequence.addAndGet(entities.size());
        Map<Entity.Reference, LocalEntityId> results = new HashMap<Entity.Reference, LocalEntityId>();
        for (Entity entity : entities) {
            // シーケンスからIDを作成して、全体のエンティティ表に登録
            LocalEntityId id = new LocalEntityId(lastSequence--);
            assert allEntities.containsKey(id) == false;
            allEntities.put(id, entity);

            // 参照から実体への表に追加
            results.put(entity.getSelfReference(), id);
        }
        return results;
    }

    /**
     * 指定のリビジョンを起点とした変更の情報をこのリポジトリ上に保存する。
     * <p>
     * 指定された変更が、リポジトリ上の最新までの変更と衝突する場合、この呼び出しは保存に失敗する。
     * </p>
     * <p>
     * 保存されたリビジョンはこのリポジトリの最新リビジョンとなる。
     * </p>
     * @param source 開始リビジョン
     * @param delta 開始リビジョンに対する変更差分
     * @return 保存したリビジョン。保存に失敗した場合は{@code null}
     */
    public Revision<LocalEntityId> commit(Revision<LocalEntityId> source, Revision.Delta<LocalEntityId> delta) {
        if (source == null) {
            throw new IllegalArgumentException("source is null"); //$NON-NLS-1$
        }
        if (delta == null) {
            throw new IllegalArgumentException("delta is null"); //$NON-NLS-1$
        }

        for (int i = 0; i < MAX_RETRY; i++) {
            // FIXME 常に現在の最新のものを使う。実際には保存先のブランチを選択したい
            Revision<LocalEntityId> head = getHeadRevision();

            // 開始リビジョンから最新までの差分を作成
            Revision.Delta<LocalEntityId> headDelta = source.createDeltaTo(head);

            // 今回の差分と、それまでに裏で行われた操作の差分を合成
            Revision.Delta<LocalEntityId> nextDelta = delta.merge(headDelta);
            if (nextDelta == null) {
                // 衝突していたら即座にあきらめる
                // FIXME 通知方法について考える
                return null;
            }

            // 開始リビジョンに合成した差分を適用
            Revision<LocalEntityId> toCommit = source.apply(nextDelta);

            // 作成したリビジョンを登録
            boolean success = addRevision(head, toCommit);
            if (success) {
                return toCommit;
            }

            // 楽観的排他制御に失敗したので、再試行する
        }

        // 再試行上限回数をオーバーしたので失敗
        // FIXME 通知方法について考える
        return null;
    }

    private boolean addRevision(Revision<LocalEntityId> currentHead, Revision<LocalEntityId> toCommit) {
        assert currentHead != null;
        assert toCommit != null;
        synchronized (revisions) {
            // タイムスタンプを比較するような感じ
            if (getHeadRevision() == currentHead) {
                revisions.addFirst(toCommit);
                return true;
            }
        }
        return false;
    }
}
