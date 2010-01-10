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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code smalltable}上のオブジェクトの内容を表現する。
 * <p>
 * それぞれのプロパティが保持する値は、下記のいずれかである。
 * </p>
 * <ul>
 * <li> {@link Entity.Reference} </li>
 * <li> {@link Integer} </li>
 * <li> {@link String} </li>
 * </ul>
 * <p>
 * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class Entity implements Serializable {

    private static final long serialVersionUID = 7398239735452818784L;

    /**
     * 自身への参照。
     */
    private transient Entity.Reference self;

    /**
     * このエンティティが保持するプロパティの一覧。
     */
    private transient Map<String, Object> properties;

    /**
     * インスタンスを生成する。
     * @param self 自身への参照
     * @param properties プロパティの一覧
     */
    Entity(Entity.Reference self, Map<String, Object> properties) {
        assert self != null;
        assert properties != null;
        this.self = self;
        this.properties = Collections.unmodifiableMap(new HashMap<String, Object>(properties));
    }

    /**
     * 自身への参照を返す。
     * @return 自身への参照
     */
    public Entity.Reference getSelfReference() {
        return this.self;
    }

    /**
     * このエンティティが保有するプロパティの一覧を返す。
     * @return このエンティティが保有するプロパティの一覧
     */
    public Map<String, Object> getPropertyMap() {
        return properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + self.hashCode();
        result = prime * result + properties.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Entity other = (Entity) obj;
        if (properties.equals(other.properties) == false) {
            return false;
        }
        if (self.equals(other.self) == false) {
            return false;
        }
        return true;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.self = (Entity.Reference) stream.readObject();
        int propertyCount = stream.readInt();
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < propertyCount; i++) {
            String name = stream.readUTF();
            Object value = stream.readObject();
            props.put(name, value);
        }
        this.properties = Collections.unmodifiableMap(props);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(self);
        stream.writeInt(properties.size());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            stream.writeUTF(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    /**
     * {@link Entity}オブジェクトを構築するためのビルダ。
     * <p>
     * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
     * </p>
     * @author ashigeru
     */
    public static class Builder {

        private Reference self;

        private Map<String, Object> properties;

        /**
         * インスタンスを生成する。
         * @param self 生成するエンティティへの参照
         */
        private Builder(Entity.Reference self) {
            this.self = self;
            this.properties = new HashMap<String, Object>();
        }

        /**
         * {@code Entity}を生成するための新しいビルダを返す。
         * @param self 生成する{@code Entity}への参照
         * @return 指定した参照に対する{@code Entity}を生成するためのこのクラスのオブジェクト
         */
        public static Builder create(Entity.Reference self) {
            return new Builder(self);
        }

        /**
         * 指定の名前を持ち、値として指定の値を持つプロパティを追加する。
         * FIXME 値の検査をしたほうがいい
         * @param name 追加するプロパティの名前
         * @param value 追加するプロパティの値
         * @return このオブジェクト
         * @throws IllegalArgumentException 指定の名前を持つプロパティがすでに追加されている場合
         */
        public Builder add(String name, Object value) {
            if (name == null) {
                throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
            }
            if (value == null) {
                throw new IllegalArgumentException("value is null"); //$NON-NLS-1$
            }
            return add0(name, value);
        }

        private Builder add0(String name, Object value) {
            assert name != null;
            assert value != null;
            if (properties.containsKey(name)) {
                throw new IllegalArgumentException(MessageFormat.format(
                    "The property \"{0}\" already exists", //$NON-NLS-N$
                    name));
            }
            properties.put(name, value);
            return this;
        }

        /**
         * これまでに設定した情報を元に、新しい{@code Entity}を構築して返す。
         * @return 構築した{@link Entity}
         */
        public Entity toEntity() {
            return new Entity(self, properties);
        }
    }

    /**
     * {@link Entity}への参照を表す。
     * @author ashigeru
     */
    public static class Reference implements Comparable<Reference>, Serializable {

        private static final long serialVersionUID = -3707674246821145278L;

        // TODO 実装に寄り過ぎなのでもうちょっとまじめに考える
        /**
         * この参照を表す値。
         */
        public final long value;

        /**
         * インスタンスを生成する。
         * @param value
         */
        public Reference(long value) {
            this.value = value;
        }

        @Override
        public int compareTo(Reference other) {
            if (value < other.value) {
                return -1;
            }
            else if (value > other.value) {
                return +1;
            }
            return 0;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (this.value ^ (this.value >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Reference other = (Reference) obj;
            if (this.value != other.value) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("Entity.Reference(%016x)", value);
        }
    }
}
