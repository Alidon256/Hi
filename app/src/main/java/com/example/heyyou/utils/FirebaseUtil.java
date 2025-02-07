package com.example.heyyou.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseUtil {

    private static final String TAG = "FirebaseUtil";
    private static final FirebaseFirestore firestoreInstance = FirebaseFirestore.getInstance();
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();


    public static FirebaseFirestore getFireStoreInstance() {
        return firestoreInstance;
    }
    // Returns the current user ID
    public static String currentUserId() {
        return firebaseAuth.getUid();
    }
    public static void uploadStatus(Context context, String statusText, Uri mediaUri, String mediaType, OnCompleteListener<DocumentReference> onCompleteListener) throws IOException {
        if (!mediaType.equals("image") && !mediaType.equals("video")) {
            return;  // Invalid media type
        }

        StorageReference statusStorageRef = firebaseStorage.getReference()
                .child("status_media")
                .child(currentUserId() + "_" + System.currentTimeMillis());

        if (mediaType.equals("video")) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, mediaUri);
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long durationInMs = Long.parseLong(durationStr);

                if (durationInMs > 30000) { // 30 seconds max
                    Log.e(TAG, "Video duration exceeds 30 seconds");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing video", e);
                return;
            } finally {
                retriever.release();
            }
        }
        statusStorageRef.putFile(mediaUri)
                .addOnSuccessListener(taskSnapshot -> {
                    statusStorageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String mediaUrl = uri.toString();

                        // Create a new document reference with an auto-generated ID
                        DocumentReference statusDocRef = firestoreInstance.collection("status").document();
                        String docId = statusDocRef.getId(); // Get the generated ID

                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("statusId", docId);  // Assign document ID as statusId
                        statusData.put("userId", currentUserId());
                        statusData.put("mediaUrl", mediaUrl);
                        statusData.put("statusText", statusText);
                        statusData.put("timestamp", FieldValue.serverTimestamp());
                        statusData.put("mediaType", mediaType);

                        // Use set() and properly handle Task<Void>
                        statusDocRef.set(statusData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Convert success to Task<DocumentReference>
                                        if (onCompleteListener != null) {
                                            onCompleteListener.onComplete(Tasks.forResult(statusDocRef));
                                        }
                                    } else {
                                        // Manually create a failure task with the same exception
                                        if (onCompleteListener != null) {
                                            onCompleteListener.onComplete(Tasks.forException(task.getException()));
                                        }
                                    }
                                });
                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to get download URL", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload media", e);
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(Tasks.forException(e)); // Handle failure properly
                    }
                });
    }

    // Checks if the user is logged in
    public static boolean isLoggedIn() {
        return currentUserId() != null;
    }

    // Returns a reference to the current user's document in Firestore
    public static DocumentReference currentUserDetails() {
        return firestoreInstance.collection("users").document(currentUserId());
    }

    // Returns a reference to the users collection
    public static CollectionReference allUserCollectionReference() {
        return firestoreInstance.collection("users");
    }

    // Returns a reference to a specific chatroom
    public static DocumentReference getChatroomReference(String chatroomId) {
        return firestoreInstance.collection("chatrooms").document(chatroomId);
    }

    // Returns a reference to messages inside a chatroom
    public static CollectionReference getChatroomMessageReference(String chatroomId) {
        return getChatroomReference(chatroomId).collection("chats");
    }

    // Generates a unique chatroom ID based on user IDs
    public static String getChatroomId(String userId1, String userId2) {
        return (userId1.hashCode() < userId2.hashCode()) ? (userId1 + "_" + userId2) : (userId2 + "_" + userId1);
    }

    // Returns a reference to all chatrooms collection
    public static CollectionReference allChatroomCollectionReference() {
        return firestoreInstance.collection("chatrooms");
    }

    // Returns a reference to the other user in a chatroom
    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        return allUserCollectionReference().document(
                userIds.get(0).equals(currentUserId()) ? userIds.get(1) : userIds.get(0)
        );
    }

    // Converts a Firestore Timestamp to a string format
    public static String timestampToString(Timestamp timestamp) {
        if (timestamp != null) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate());
        } else {
            return "No timestamp available";  // or any default string you prefer
        }
    }


    // Logs out the current user and updates online status
    public static void logout() {
        setUserOnlineStatus(false); // Set user offline before logout
        firebaseAuth.signOut();
    }
    public static void getUserName(String userId, UserNameCallback callback) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        callback.onSuccess(name != null ? name : "Unknown");
                    } else {
                        callback.onFailure();
                    }
                })
                .addOnFailureListener(e -> callback.onFailure());
    }

    public interface UserNameCallback {
        void onSuccess(String name);
        void onFailure();
    }
    // Returns a reference to the current user's profile picture storage location
    public static StorageReference getCurrentProfilePicStorageRef() {
        return firebaseStorage.getReference().child("profile_pic").child(currentUserId());
    }

    // Returns a reference to another user's profile picture storage location
    public static StorageReference getOtherProfilePicStorageRef(String otherUserId) {
        return firebaseStorage.getReference().child("profile_pic").child(otherUserId);
    }

    // 🔥 NEW FEATURE: Update Online Status 🔥
    public static void setUserOnlineStatus(boolean isOnline) {
        String userId = currentUserId();
        if (userId == null) return;

        DocumentReference userRef = allUserCollectionReference().document(userId);
        userRef.update("isOnline", isOnline)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User online status updated: " + isOnline))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status", e));
    }

    // 🔥 NEW FEATURE: Listen for Online Status 🔥
    public static void listenForUserStatus(String userId, OnlineStatusCallback callback) {
        allUserCollectionReference().document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching online status", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        boolean isOnline = snapshot.getBoolean("isOnline") != null && Boolean.TRUE.equals(snapshot.getBoolean("isOnline"));
                        callback.onStatusChanged(isOnline);
                    }
                });
    }

    // Callback Interface for Online Status
    public interface OnlineStatusCallback {
        void onStatusChanged(boolean isOnline);
    }
}
