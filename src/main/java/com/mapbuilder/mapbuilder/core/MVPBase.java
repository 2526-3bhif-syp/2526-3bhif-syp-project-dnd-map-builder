package com.mapbuilder.mapbuilder.core;

public interface MVPBase {
    interface View {
        // Base view interface
    }

    interface Presenter<V extends View> {
        void setView(V view);
        V getView();
    }
}
