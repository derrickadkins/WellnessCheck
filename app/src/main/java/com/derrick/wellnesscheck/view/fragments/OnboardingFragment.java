//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.derrick.wellnesscheck.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;

import com.derrick.wellnesscheck.controller.OnboardingEngine;
import com.ramotion.paperonboarding.PaperOnboardingFragment;
import com.ramotion.paperonboarding.PaperOnboardingPage;
import com.ramotion.paperonboarding.R.id;
import com.ramotion.paperonboarding.R.layout;
import com.ramotion.paperonboarding.listeners.PaperOnboardingOnChangeListener;
import com.ramotion.paperonboarding.listeners.PaperOnboardingOnLeftOutListener;
import com.ramotion.paperonboarding.listeners.PaperOnboardingOnRightOutListener;
import java.util.ArrayList;

public class OnboardingFragment extends PaperOnboardingFragment {
    private static final String ELEMENTS_PARAM = "elements";
    private PaperOnboardingOnChangeListener mOnChangeListener;
    private PaperOnboardingOnRightOutListener mOnRightOutListener;
    private PaperOnboardingOnLeftOutListener mOnLeftOutListener;
    private ArrayList<PaperOnboardingPage> mElements;
    public OnboardingEngine onboardingEngine;

    public OnboardingFragment() {
    }

    public static OnboardingFragment newInstance(ArrayList<PaperOnboardingPage> elements) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ELEMENTS_PARAM, elements);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.getArguments() != null) {
            this.mElements = (ArrayList)this.getArguments().get(ELEMENTS_PARAM);
        }

    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(layout.onboarding_main_layout, container, false);
        onboardingEngine = new OnboardingEngine(view.findViewById(id.onboardingRootView), this.mElements, this.getActivity().getApplicationContext());
        onboardingEngine.setOnChangeListener(this.mOnChangeListener);
        onboardingEngine.setOnLeftOutListener(this.mOnLeftOutListener);
        onboardingEngine.setOnRightOutListener(this.mOnRightOutListener);
        return view;
    }

    public void setElements(ArrayList<PaperOnboardingPage> elements) {
        this.mElements = elements;
    }

    public ArrayList<PaperOnboardingPage> getElements() {
        return this.mElements;
    }

    public void setOnChangeListener(PaperOnboardingOnChangeListener onChangeListener) {
        this.mOnChangeListener = onChangeListener;
    }

    public void setOnRightOutListener(PaperOnboardingOnRightOutListener onRightOutListener) {
        this.mOnRightOutListener = onRightOutListener;
    }

    public void setOnLeftOutListener(PaperOnboardingOnLeftOutListener onLeftOutListener) {
        this.mOnLeftOutListener = onLeftOutListener;
    }
}