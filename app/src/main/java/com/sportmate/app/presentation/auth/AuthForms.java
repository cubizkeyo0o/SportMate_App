package com.sportmate.app.presentation.auth;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.presentation.common.ErrorMessages;
import com.sportmate.app.presentation.common.UiText;

/** View helpers shared by the login and register forms. */
final class AuthForms {
    private AuthForms() {
    }

    static void showFieldError(@NonNull TextInputLayout layout, @Nullable UiText error) {
        layout.setError(error == null ? null : error.resolve(layout.getContext()));
    }

    static void showGeneralError(@NonNull TextView view, @Nullable AppError error) {
        if (error == null) {
            view.setVisibility(View.GONE);
            view.setText(null);
        } else {
            view.setText(ErrorMessages.resolve(view.getContext(), error));
            view.setVisibility(View.VISIBLE);
        }
    }

    /** Calls {@code onEdited} when the user changes the text, not when it is restored. */
    static void onUserEdit(@NonNull EditText editText, @NonNull Runnable onEdited) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.hasFocus()) {
                    onEdited.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    static String text(@NonNull EditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString();
    }

    static void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
