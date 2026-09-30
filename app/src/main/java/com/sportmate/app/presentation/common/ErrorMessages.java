package com.sportmate.app.presentation.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;

/**
 * Turns an {@link AppError} into text for the user. The backend's message is preferred for client
 * errors, where it is written for clients; otherwise a generic message per error kind is used.
 */
public final class ErrorMessages {
    private ErrorMessages() {
    }

    @NonNull
    public static String resolve(@NonNull Context context, @NonNull AppError error) {
        String backendMessage = error.getMessage();
        if (usesBackendMessage(error.getKind()) && backendMessage != null && !backendMessage.isEmpty()) {
            return backendMessage;
        }
        return context.getString(fallbackFor(error.getKind()));
    }

    private static boolean usesBackendMessage(AppError.Kind kind) {
        return kind != AppError.Kind.NETWORK
                && kind != AppError.Kind.SERVER
                && kind != AppError.Kind.UNKNOWN;
    }

    @StringRes
    private static int fallbackFor(AppError.Kind kind) {
        switch (kind) {
            case NETWORK:
                return R.string.error_network;
            case VALIDATION:
                return R.string.error_validation;
            case UNAUTHORIZED:
                return R.string.error_unauthorized;
            case FORBIDDEN:
                return R.string.error_forbidden;
            case NOT_FOUND:
                return R.string.error_not_found;
            case CONFLICT:
                return R.string.error_conflict;
            case BUSINESS_RULE:
                return R.string.error_business_rule;
            case SERVER:
                return R.string.error_server;
            default:
                return R.string.error_unknown;
        }
    }
}
