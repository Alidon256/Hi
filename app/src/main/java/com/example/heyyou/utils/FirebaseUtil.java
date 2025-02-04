package com.example.heyyou.utils;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FirebaseUtil {

    private static final String TAG = "FirebaseUtil";
    private static final FirebaseFirestore firestoreInstance = FirebaseFirestore.getInstance();
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();

    // Returns the current user ID
    public static String currentUserId() {
        return firebaseAuth.getUid();
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
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate());
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
