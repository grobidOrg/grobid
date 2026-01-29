/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grobid.core.analyzers;

/**
 * @author Chihebeddine Ammar
 */
public class ArabicChars {

    /**
     * Method for mapping some Arabic characters to their equivalent ASCII codes.
     */
    public static char arabicCharacters(char c) {
        char car;
        switch (c) {
            case '،' :
                car = ',';
                break;
            case '؛' :
                car = ';';
                break;
            case '؟' :
                car = '?';
                break;
            case '٠' :
                car = '0';
                break;
            case '١' :
                car = '1';
                break;
            case '٢' :
                car = '2';
                break;
            case '٣' :
                car = '3';
                break;
            case '٤' :
                car = '4';
                break;
            case '٥' :
                car = '5';
                break;
            case '٦' :
                car = '6';
                break;
            case '٧' :
                car = '7';
                break;
            case '٨' :
                car = '8';
                break;
            case '٩' :
                car = '9';
                break;
            default :
                car = c;
                break;
        }
        return car;
    }

}
