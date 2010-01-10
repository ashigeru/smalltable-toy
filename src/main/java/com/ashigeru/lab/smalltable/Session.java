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

import java.util.Collection;

/**
 * {@code smalltable}を操作するためのセッション。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public interface Session {

    /**
     * 指定の個数の参照を新しく割り当てて返す。
     * @return 割り当てられた参照
     */
    Entity.Reference allocateReference();

    /**
     * 現在のセッションにおいて、指定の参照に対応するエンティティを返す。
     * @param reference 対象の参照
     * @return 対応するエンティティ
     */
    Entity resolve(Entity.Reference reference);

    /**
     * 指定の名前に、指定の参照を結びつける。
     * @param name 対象の名前
     * @param reference 名前に結びつける参照。{@code null}が指定された場合は参照を解除する
     */
    void bind(String name, Entity.Reference reference);

    /**
     * 指定の名前を持つ参照を返す。
     * @param name 対象の名前
     * @return 対応する参照、存在しない場合は{@code null}
     */
    Entity.Reference getBound(String name);

    /**
     * 現在のセッションを永続化する。
     * @param modified このセッションで更新されたエンティティの一覧
     */
    void save(Collection<? extends Entity> modified);
}
