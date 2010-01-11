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
package com.ashigeru.lab.smalltable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ashigeru.lab.smalltable.Entity.Reference;

/**
 * 一度のセッションで作成される{@code smalltable}上のリビジョン情報。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 * @param <T> このリビジョンで取り扱うエンティティ識別子の種類
 */
public class Revision<T> implements Serializable {

    private static final long serialVersionUID = -445530513090135218L;

    /**
     * 名前つき参照の一覧表。
     */
    private Map<String, Entity.Reference> bindings;

    /**
     * エンティティへの参照をエンティティへの実体にマッピングする表。
     */
    private Map<Entity.Reference, T> entities;

    /**
     * インスタンスを生成する。
     * @param bindings このリビジョンから利用可能な名前つき参照の一覧表
     * @param entities このリビジョンから利用可能なエンティティへの参照とその実体への表
     */
    public Revision(Map<String, Entity.Reference> bindings, Map<Entity.Reference, T> entities) {
        this();
        if (bindings == null) {
            throw new IllegalArgumentException("bindings is null"); //$NON-NLS-1$
        }
        if (entities == null) {
            throw new IllegalArgumentException("entities is null"); //$NON-NLS-1$
        }
        this.bindings = new HashMap<String, Reference>(bindings);
        this.entities = new HashMap<Reference, T>(entities);
    }

    /**
     * インスタンスを生成する。
     */
    private Revision() {
        return;
    }

    /**
     * このリビジョンにおいて指定の名前がつけられた参照を返す。
     * @param name 対象の名前
     * @return 指定の名前つき参照、存在しない場合は{@code null}
     */
    public Entity.Reference getBinding(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        return bindings.get(name);
    }

    /**
     * このリビジョンにおいて指定の参照に対するエンティティの識別子を返す。
     * @param reference 対象の参照
     * @return 対応するエンティティの識別子、存在しない場合は{@code null}
     */
    public T getId(Entity.Reference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("reference is null"); //$NON-NLS-1$
        }
        return entities.get(reference);
    }

    /**
     * このリビジョンを起点として、指定のリビジョンまでの変更を計算して返す。
     * @param target 変更後のリビジョン
     * @return このリビジョンから指定のリビジョンまでの変更情報
     */
    public Revision.Delta<T> createDeltaTo(Revision<T> target) {
        if (target == null) {
            throw new IllegalArgumentException("target is null"); //$NON-NLS-1$
        }
        Map<String, Reference> bindingDelta = difference(bindings, target.bindings);
        Map<Reference, T> entityDelta = difference(entities, target.entities);
        return new Revision.Delta<T>(bindingDelta, entityDelta);
    }

    private static <K extends Comparable<K>, V> Map<K, V> difference(Map<K, V> from, Map<K, V> to) {
        assert to != null;
        assert from != null;
        Set<K> saw = new HashSet<K>();
        Map<K, V> result = new HashMap<K, V>();

        // from -> to の変化をトラッキング
        for (Map.Entry<K, V> entry : to.entrySet()) {
            K key = entry.getKey();
            V fromValue = from.get(key);
            V toValue = entry.getValue();
            if (fromValue == null) {
                if (toValue != null) {
                    result.put(key, toValue);
                }
            }
            else if (fromValue.equals(toValue) == false) {
                result.put(key, toValue);
            }

            // 一度見たキーは保持しておく
            saw.add(key);
        }

        // from に一度も見てないキーがあればtoで削除された
        for (Map.Entry<K, V> entry : from.entrySet()) {
            K key = entry.getKey();
            if (saw.contains(key) == false) {
                result.put(key, null);
            }
        }
        return result;
    }

    /**
     * このリビジョンに指定の変更情報を適用した、新しいリビジョンを返す。
     * @param delta 対象の変更情報
     * @return 対象の変更情報を適用した新しいリビジョン
     */
    public Revision<T> apply(Revision.Delta<T> delta) {
        if (delta == null) {
            throw new IllegalArgumentException("delta is null"); //$NON-NLS-1$
        }
        Revision<T> results = new Revision<T>();
        results.bindings = apply(bindings, delta.bindings);
        results.entities = apply(entities, delta.entities);
        return results;
    }

    private static <K extends Comparable<K>, V> Map<K, V> apply(Map<K, V> origin, Map<K, V> delta) {
        assert origin != null;
        assert delta != null;
        if (delta.isEmpty()) {
            return origin;
        }
        Map<K, V> result = new HashMap<K, V>(origin);
        for (Map.Entry<K, V> entry : delta.entrySet()) {
            if (entry.getValue() == null) {
                result.remove(entry.getKey());
            }
            else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * リビジョン間の差分。
     * <p>
     * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
     * </p>
     * @author ashigeru
     * @param <T> この差分に関連のあるリビジョンで取り扱う識別子の型
     */
    public static class Delta<T> implements Serializable {

        private static final long serialVersionUID = -9193626137447698064L;

        /**
         * 変更があった名前つき参照の表。
         * <p>
         * 削除された名前つき参照については、名前に対する値が{@code null}となる。
         * </p>
         */
        Map<String, Entity.Reference> bindings;

        /**
         * 変更があったエンティティの識別子表。
         * <p>
         * 削除された識別子については、参照に対する識別子が{@code null}となる。
         * </p>
         */
        Map<Entity.Reference, T> entities;

        /**
         * インスタンスを生成する。
         * @param bindings 変更があった名前つき参照の表。削除された名前つき参照については、名前に対する値を{@code null}で表す
         * @param entities 変更があったエンティティの識別子表。削除された識別子については、参照に対する識別子を{@code null}で表す
         */
        public Delta(Map<String, Entity.Reference> bindings, Map<Entity.Reference, T> entities) {
            if (bindings == null) {
                throw new IllegalArgumentException("bindings is null"); //$NON-NLS-1$
            }
            if (entities == null) {
                throw new IllegalArgumentException("entities is null"); //$NON-NLS-1$
            }
            this.bindings = bindings;
            this.entities = entities;
        }

        /**
         * この変更に、指定された名前つき参照またはエンティティの参照がひとつでも含まれる場合に{@code true}を返す。
         * @param bindingsChanged 名前つき参照の名前一覧
         * @param entityChanged エンティティの識別子表の参照一覧
         * @return いずれかの名前、または参照が含まれる場合に{@code true}
         */
        public boolean conflictsWith(Set<String> bindingsChanged, Set<Entity.Reference> entityChanged) {
            if (bindingsChanged == null) {
                throw new IllegalArgumentException("bindingsChanged is null"); //$NON-NLS-1$
            }
            if (entityChanged == null) {
                throw new IllegalArgumentException("entityChanged is null"); //$NON-NLS-1$
            }
            if (conflictsAny(bindings.keySet(), bindingsChanged)) {
                return true;
            }
            if (conflictsAny(entities.keySet(), entityChanged)) {
                return true;
            }
            return false;
        }

        private static <K> boolean conflictsAny(Set<K> a, Set<K> b) {
            assert a != null;
            assert b != null;
            if (a.size() < b.size()) {
                for (K k : a) {
                    if (b.contains(k)) {
                        return true;
                    }
                }
            }
            else {
                for (K k : b) {
                    if (a.contains(k)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * この変更に指定した変更を合成した、新しい変更を返す。
         * <p>
         * 2つの変更は互いに疎でなければならない。
         * つまり、同一の名前つき参照および識別子表上の参照において、2つに共通する変更があってはならない。
         * そのような変更を衝突と呼び、衝突を含む場合にこの呼び出しは{@code null}を返す。
         * </p>
         * @param other 合成する変更
         * @return 合成後の変更、いずれかの変更が衝突する場合には{@code null}
         */
        public Delta<T> merge(Delta<T> other) {
            if (other == null) {
                throw new IllegalArgumentException("other is null"); //$NON-NLS-1$
            }
            if (conflictsAny(bindings.keySet(), other.bindings.keySet())) {
                return null;
            }
            if (conflictsAny(entities.keySet(), other.entities.keySet())) {
                return null;
            }
            Map<String, Entity.Reference> newBindings = new HashMap<String, Reference>();
            newBindings.putAll(bindings);
            newBindings.putAll(other.bindings);

            Map<Entity.Reference, T> newEntities = new HashMap<Reference, T>();
            newEntities.putAll(entities);
            newEntities.putAll(other.entities);

            return new Delta<T>(newBindings, newEntities);
        }
    }
}
