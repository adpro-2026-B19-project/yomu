package id.ac.ui.cs.advprog.yomu.auth.service;

import java.util.UUID;

public interface ProfileService {

    UpdateProfileResult updateProfile(UpdateProfileRequest request);

    DeleteAccountResult deleteOwnAccount(DeleteAccountRequest request);

    record UpdateProfileRequest(UUID userId, String username, String displayName, Long phoneNumber) {}

    record DeleteAccountRequest(UUID userId, String password) {}

    record UpdatedProfileSummary(String username, String email, String displayName, Long phoneNumber) {}

    record UpdateProfileResult(
            boolean success,
            String errorCode,
            String errorMessage,
            UpdatedProfileSummary updatedProfile
    ) {
        public static UpdateProfileResult successResult(UpdatedProfileSummary updatedProfile) {
            return new UpdateProfileResult(true, null, null, updatedProfile);
        }

        public static UpdateProfileResult failureResult(String errorCode, String errorMessage) {
            return new UpdateProfileResult(false, errorCode, errorMessage, null);
        }
    }

    record DeleteAccountResult(boolean success, String errorCode, String errorMessage) {
        public static DeleteAccountResult successResult() {
            return new DeleteAccountResult(true, null, null);
        }

        public static DeleteAccountResult failureResult(String errorCode, String errorMessage) {
            return new DeleteAccountResult(false, errorCode, errorMessage);
        }
    }
}
