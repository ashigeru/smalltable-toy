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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ashigeru.lab.smalltable.Entity;

/**
 * {@link SmallTable}上で利用可能なオブジェクト。
 * <p>
 * このオブジェクトを生成するには、{@link SmallTable#newObject()}を利用する。
 * </p>
 * <p>
 * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class StObject {

    private SmallTable table;

    private Entity.Reference reference;

    private Map<String, Object> source;

    private Map<String, Object> modified;

    /**
     * インスタンスを生成する。
     * @param table このオブジェクトを管理するテーブル
     * @param reference このオブジェクトへの参照
     */
    StObject(SmallTable table, Entity.Reference reference) {
        if (table == null) {
            throw new IllegalArgumentException("table is null"); //$NON-NLS-1$
        }
        if (reference == null) {
            throw new IllegalArgumentException("reference is null"); //$NON-NLS-1$
        }
        this.table = table;
        this.source = Collections.emptyMap();
        this.modified = new HashMap<String, Object>();
    }

    StObject(SmallTable table, Entity source) {
        if (table == null) {
            throw new IllegalArgumentException("table is null"); //$NON-NLS-1$
        }
        if (source == null) {
            throw new IllegalArgumentException("source is null"); //$NON-NLS-1$
        }
        this.table = table;
        this.source = source.getPropertyMap();
        this.modified = new HashMap<String, Object>();
    }

    /**
     * このオブジェクトへの参照を返す。
     * @return このオブジェクトへの参照
     */
    Entity.Reference getReference() {
        return this.reference;
    }

    /**
     * このオブジェクトをEntityに変換して返す。
     * @return このオブジェクトに対応するエンティティ
     */
    Entity toEntity() {
        Entity.Builder builder = Entity.Builder.create(reference);

        // 先に変更一覧のプロパティを追加
        for (Map.Entry<String, Object> entry : modified.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                builder.add(entry.getKey(), value);
            }
        }

        // オリジナルのうち、変更一覧に含まれていないもののみを追加
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (modified.containsKey(entry.getKey()) == false) {
                Object value = entry.getValue();
                if (value != null) {
                    builder.add(entry.getKey(), value);
                }
            }
        }
        return builder.toEntity();
    }

    /**
     * このオブジェクトが現在のセッションにおいて変更されている場合のみ{@code true}を返す。
     * @return このオブジェクトが現在のセッションにおいて変更されている場合のみ{@code true}
     */
    boolean isModified() {
        // まず、変更一覧が空なら変更はない
        if (modified.isEmpty()) {
            return false;
        }

        // それぞれのプロパティについて比較
        Iterator<Map.Entry<String, Object>> iter = modified.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Object> entry = iter.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            Object original = source.get(name);

            // オリジナルから新しく出現したか？
            if (original == null) {
                if (value != null) {
                    return true;
                }
            }

            // オリジナルの値が変更されたか？
            else if (original.equals(value) == false) {
                return true;
            }

            // 同じだったプロパティを変更一覧から除去
            iter.remove();
        }

        // すべて同じなので変更なし
        return false;
    }

    /**
     * このオブジェクトを管理する{@code smalltable}オブジェクトを返す。
     * @return このオブジェクトを管理する{@code smalltable}オブジェクト
     */
    public SmallTable getTable() {
        return this.table;
    }

    /**
     *
     * @param name 対象のプロパティ名
     * @param value プロパティに設定する値、プロパティを削除する場合は{@code null}
     */
    public void setProperty(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        // 書き込みは常に変更一覧に行う
        modified.put(name, toPropertyValue(value));
    }

    /**
     *
     * @param name 対象のプロパティ名
     * @return 対応するプロパティの値、存在しない場合は{@code null}
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
        }
        // 読み出しは変更一覧 -> オリジナルの順
        Object value = getPropertyValue(name);
        return toUserValue(value);
    }

    private Object toPropertyValue(Object userValue) {
        if (userValue == null) {
            return null;
        }
        if (userValue instanceof String) {
            return userValue;
        }
        if (userValue instanceof Integer) {
            return userValue;
        }
        if (userValue instanceof StObject) {
            StObject sto = (StObject) userValue;
            // オブジェクトは同一のテーブルから生成されている必要がある
            if (getTable().equals(sto.getTable()) == false) {
                throw new IllegalArgumentException(MessageFormat.format(
                    "{0} must be created from the same SmallTable with {1}",
                    userValue,
                    this));
            }
            // オブジェクトそのものではなく、参照を保持する
            return sto.getReference();
        }
        throw new IllegalArgumentException(MessageFormat.format(
            "Property type {0} is not allowed for a StObject property",
            userValue.getClass().getName()));
    }

    private Object toUserValue(Object propertyValue) {
        if (propertyValue instanceof Entity.Reference) {
            return getTable().resolve((Entity.Reference) propertyValue);
        }
        return propertyValue;
    }

    private Object getPropertyValue(String name) {
        assert name != null;
        if (modified.containsKey(name)) {
            return modified.get(name);
        }
        return source.get(name);
    }
}
