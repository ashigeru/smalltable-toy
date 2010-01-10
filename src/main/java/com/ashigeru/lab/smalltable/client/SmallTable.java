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
package com.ashigeru.lab.smalltable.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ashigeru.lab.smalltable.Entity;
import com.ashigeru.lab.smalltable.Session;

/**
 * {@code smalltable}のクライアントインターフェース。
 * <p>
 * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class SmallTable {

    /**
     * このテーブルがラップするセッション。
     */
    private Session session;

    /**
     * このテーブルによって生成されたオブジェクトへの参照。
     */
    private Set<Entity.Reference> created;

    /**
     * セッションから復元したオブジェクトの一覧。
     */
    private Map<Entity.Reference, StObject> objects;

    /**
     * インスタンスを生成する。
     * @param session このテーブルの情報を持つセッション
     */
    public SmallTable(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session is null"); //$NON-NLS-1$
        }
        this.session = session;
        this.created = new HashSet<Entity.Reference>();
        this.objects = new HashMap<Entity.Reference, StObject>();
    }

    /**
     * 指定の名前で登録されたルートオブジェクトを返す。
     * @param name 取得するルートオブジェクトの名称
     * @return 指定のルートオブジェクト。存在しない場合は{@code null}
     */
    public StObject getRootObject(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        Entity.Reference reference = session.getBound(name);
        if (reference == null) {
            return null;
        }
        return resolve(reference);
    }

    /**
     * 指定のオブジェクトを、指定の名前でルートオブジェクトとして登録する。
     * <p>
     * {@code object}に{@code null}が指定された場合、{@code name}に指定したルートオブジェクトを削除する。
     * ルートオブジェクトを削除しても、オブジェクト自体に影響はない。
     * </p>
     * @param name 登録するルートオブジェクトの名称
     * @param object 登録するルートオブジェクト、{@code null}の場合はルートオブジェクトを削除する
     * @throws IllegalArgumentException このテーブルと関係のないオブジェクトが指定された場合
     */
    public void setRootObject(String name, StObject object) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        if (object == null) {
            session.bind(name, null);
        }
        else {
            if (this.equals(object.getTable()) == false) {
                throw new IllegalArgumentException();
            }
            session.bind(name, object.getReference());
        }
    }

    /**
     * 新しいオブジェクトをこのテーブル上に作成して返す。
     * @return 生成した新しいオブジェクト
     */
    public StObject newObject() {
        Entity.Reference reference = session.allocateReference();
        StObject object = new StObject(this, reference);
        objects.put(reference, object);
        return object;
    }

    /**
     * このテーブルへのこれまでの変更を保存する。
     * <p>
     * FIXME これ以降はこのオブジェクトを利用できない。トイ実装なのでそのあたりのチェックはやっていない
     * </p>
     * <p>
     * FIXME 現在のつくりだと保存に失敗した場合にどうしようもなくなるから、何とかする方法を考えたいね
     * </p>
     * <p>
     * TODO 保存先のブランチとか指定したいね
     * </p>
     */
    public void save() {
        List<Entity> modified = computeModified();
        session.save(modified);
    }

    private List<Entity> computeModified() {
        List<Entity> results = new ArrayList<Entity>();
        for (StObject object : objects.values()) {
            if (isCreatedOrModified(object)) {
                results.add(object.toEntity());
            }
        }
        return results;
    }

    private boolean isCreatedOrModified(StObject object) {
        assert object != null;
        // 新規作成されたら常に変更扱いにする
        if (created.contains(object.getReference())) {
            return true;
        }
        return object.isModified();
    }

    /**
     * 指定の参照を解決し、オブジェクトを構築して返す。
     * @param reference 解決する参照
     * @return 結果のオブジェクト
     */
    StObject resolve(Entity.Reference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("reference is null"); //$NON-NLS-1$
        }

        // キャッシュされたオブジェクトから探す
        StObject object = objects.get(reference);
        if (object != null) {
            return object;
        }

        // なければセッションから取得してくる
        Entity entity = session.resolve(reference);
        StObject restored = new StObject(this, entity);
        objects.put(reference, restored);
        return restored;
    }
}
