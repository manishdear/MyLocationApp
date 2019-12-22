package com.unofficialcoder.mylocationapp.ui.charge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChargeViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ChargeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}