package com.grahambartley.client.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientDraftProfileTest {
  @Test
  void mutatorsMarkDraftDirty() {
    LootLockProfile source = LootLockProfile.createDefault();
    ClientDraftProfile draft = new ClientDraftProfile(source.getId(), 4L, source);

    draft.setName("Builder");
    draft.setMode(FilterMode.ALLOWLIST);
    draft.setRejectedItemAction(RejectedItemAction.DELETE);
    draft.setEnabled(false);
    draft.setRules(List.of(new RuleEntry("minecraft:diamond")));

    assertTrue(draft.isDirty());
    assertEquals("Builder", draft.getDraft().getName());
    assertEquals(FilterMode.ALLOWLIST, draft.getDraft().getMode());
    assertEquals(RejectedItemAction.DELETE, draft.getDraft().getRejectedItemAction());
    assertEquals(1, draft.getDraft().getRules().size());
  }

  @Test
  void constructorClonesInputProfile() {
    LootLockProfile source = LootLockProfile.createDefault();
    source.setName("Original");
    source.setRules(List.of(new RuleEntry("minecraft:stone")));

    ClientDraftProfile draft = new ClientDraftProfile(UUID.randomUUID(), 2L, source);
    source.setName("Changed Later");
    source.setRules(List.of());

    assertEquals("Original", draft.getDraft().getName());
    assertEquals(1, draft.getDraft().getRules().size());
  }

  @Test
  void noOpMutationsDoNotMarkDirty() {
    LootLockProfile source = LootLockProfile.createDefault();
    source.setRules(List.of());

    ClientDraftProfile draft = new ClientDraftProfile(source.getId(), 2L, source);
    draft.setName(source.getName());
    draft.setMode(source.getMode());
    draft.setRejectedItemAction(source.getRejectedItemAction());
    draft.setEnabled(source.isEnabled());
    draft.setRules(null);

    assertFalse(draft.isDirty());
  }

  @Test
  void constructorRejectsNullSourceProfile() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ClientDraftProfile(UUID.randomUUID(), 1L, null));

    assertEquals("source must not be null", exception.getMessage());
  }
}
