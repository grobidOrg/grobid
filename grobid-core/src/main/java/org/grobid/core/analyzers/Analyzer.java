/*
 * Copyright 2008-2026 GROBID contributors
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
package org.grobid.core.analyzers;

import java.util.List;

import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;

/**
 * Abstract analyzer for tokenizing/filtering text.
 *
 */
public interface Analyzer {

    List<String> tokenize(String text);

    List<String> tokenize(String text, Language lang);

    List<String> retokenize(List<String> chunks);

    List<LayoutToken> tokenizeWithLayoutToken(String text);

    List<LayoutToken> retokenizeFromLayoutToken(List<LayoutToken> tokens);

    List<String> retokenizeSubdigits(List<String> chunks);

    List<LayoutToken> retokenizeSubdigitsWithLayoutToken(List<String> chunks);

    List<LayoutToken> retokenizeSubdigitsFromLayoutToken(List<LayoutToken> tokens);

    String getName();
}
