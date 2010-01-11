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

import com.ashigeru.lab.smalltable.Revision;

/**
 * {@link Revision}が利用するエンティティの識別子。
 * <p>
 * 特別な指定がない限り、このクラスのすべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class LocalEntityId {

    private long numeric;

    /**
     * インスタンスを生成する。
     * @param numeric 数値によって表現された識別
     */
    public LocalEntityId(long numeric) {
        this.numeric = numeric;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (numeric ^ (numeric >>> 32));
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
        LocalEntityId other = (LocalEntityId) obj;
        if (numeric != other.numeric) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("LocalEntityId(%016x)", numeric);
    }
}
