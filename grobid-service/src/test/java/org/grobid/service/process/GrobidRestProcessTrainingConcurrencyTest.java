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
package org.grobid.service.process;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the per-model "already training" guard in {@link GrobidRestProcessTraining}.
 * The guard prevents starting a second training for a model whose training is still running.
 * These tests exercise the claim/release logic directly (no Wapiti, no GROBID home), which is
 * exactly the behaviour the guard is responsible for.
 */
public class GrobidRestProcessTrainingConcurrencyTest {

    private GrobidRestProcessTraining target;

    @Before
    public void setUp() {
        target = new GrobidRestProcessTraining();
    }

    @Test
    public void tryClaim_firstClaimForAModel_succeeds() {
        assertTrue(target.tryClaim("header", "token-1"));
    }

    @Test
    public void tryClaim_secondClaimForSameModelWhileRunning_isRejected() {
        assertTrue("first claim should win", target.tryClaim("header", "token-1"));
        assertFalse("a second claim for the same model must be rejected", target.tryClaim("header", "token-2"));
    }

    @Test
    public void tryClaim_differentModels_doNotBlockEachOther() {
        assertTrue(target.tryClaim("header", "token-1"));
        // a flavor variant is a distinct model (different output file), so it is allowed
        assertTrue(target.tryClaim("header-light", "token-2"));
        assertTrue(target.tryClaim("citation", "token-3"));
    }

    @Test
    public void tryClaim_afterRelease_isAllowedAgain() {
        assertTrue(target.tryClaim("header", "token-1"));
        assertFalse(target.tryClaim("header", "token-2"));

        target.release("header", "token-1");

        assertTrue("once released, the model can be claimed again", target.tryClaim("header", "token-2"));
    }

    @Test
    public void release_unknownModel_isANoOp() {
        // releasing something that was never claimed must not throw and must not corrupt state
        target.release("never-claimed", "token-1");
        assertTrue(target.tryClaim("never-claimed", "token-1"));
    }

    @Test
    public void release_byNonOwnerToken_doesNotFreeTheClaim() {
        // Guards the New-A regression: a stale kill/cleanup of an old token must not free a claim
        // that a newer training of the same model owns.
        assertTrue(target.tryClaim("header", "owner-token"));

        // an unrelated token attempts to release — the owner check must make this a no-op
        target.release("header", "stale-token");

        assertFalse(
                "the claim owned by owner-token must survive a release by a different token",
                target.tryClaim("header", "another-token"));
    }
}
