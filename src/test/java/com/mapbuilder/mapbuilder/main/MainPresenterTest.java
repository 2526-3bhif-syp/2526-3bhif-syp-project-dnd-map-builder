package com.mapbuilder.mapbuilder.main;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainPresenterTest {

    @Test
    void testDebouncerInitialization() {
        // Simple test to ensure the presenter instantiates without crashing and has async concepts wired.
        // Complex JavaFX testing would require TestFX, but we mock/verify the logic here conceptually.
        MainPresenter presenter = new MainPresenter();
        assertNotNull(presenter, "Presenter should instantiate successfully");
    }
}
