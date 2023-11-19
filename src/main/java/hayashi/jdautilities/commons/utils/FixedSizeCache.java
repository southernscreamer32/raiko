/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hayashi.jdautilities.commons.utils;

import java.util.HashMap;
import java.util.Map;

public class FixedSizeCache<K, V> {
    private final Map<K, V> map;
    private final K[] keys;
    private int currIndex;

    @SuppressWarnings("unchecked")
    public FixedSizeCache(int size) {
        map = new HashMap<>();
        if (size < 1)
            throw new IllegalArgumentException("Cache size must be at least 1!");
        keys = (K[]) new Object[size];
    }

    public void add(K key, V value) {
        if (keys[currIndex] != null)
            map.remove(keys[currIndex]);
        map.put(key, value);
        keys[currIndex] = key;
        currIndex = (currIndex + 1) % keys.length;
    }

    public boolean contains(K key) {
        return map.containsKey(key);
    }

    public V get(K key) {
        return map.get(key);
    }
}