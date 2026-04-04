package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;

public class MainPresenter implements MVPBase.Presenter<MainView> {
    
    private MainView view;

    @Override
    public void setView(MainView view) {
        this.view = view;
        bind();
    }

    @Override
    public MainView getView() {
        return view;
    }

    private void bind() {
        // Bind UI actions to presenter logic
    }
}
