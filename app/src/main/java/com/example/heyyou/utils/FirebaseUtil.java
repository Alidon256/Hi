package com.example.heyyou.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
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

        // Upload media
        statusStorageRef.putFile(mediaUri)
                .addOnSuccessListener(taskSnapshot -> {
                    statusStorageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String mediaUrl = uri.toString();

                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("userId", currentUserId());
                        statusData.put("mediaUrl", mediaUrl);
                        statusData.put("statusText", statusText);
                        statusData.put("timestamp", FieldValue.serverTimestamp());
                        statusData.put("mediaType", mediaType);

                        // Upload data to Firestore
                        firestoreInstance.collection("status")
                                .add(statusData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        onCompleteListener.onComplete(task); // Convert task to Void task
                                    } else {
                                        onCompleteListener.onComplete(task);
                                    }
                                });
                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to get download URL", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload media", e);
                    onCompleteListener.onComplete(null);  // Handle failure
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
                        boolean isOnline = snapshot.getBoolean("isOnline") != null && snapshot.getBoolean("isOnline");
                        callback.onStatusChanged(isOnline);
                    }
                });
    }

    // Callback Interface for Online Status
    public interface OnlineStatusCallback {
        void onStatusChanged(boolean isOnline);
    }
}
