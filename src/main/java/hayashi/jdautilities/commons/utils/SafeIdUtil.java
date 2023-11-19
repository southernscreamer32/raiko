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

public class SafeIdUtil {

    public static long safeConvert(String id) {
        try {
            return Math.max(Long.parseLong(id.trim()),0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean checkId(String id) {
        try {
            return Long.parseLong(id.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}