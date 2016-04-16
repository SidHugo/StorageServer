package ru.mephi.var.data;

import java.util.UUID;

/**
 * Created by Sid_Hugo on 16.04.2016.
 */
public class TreeNode {
    private UUID encryptedId;
    private Data data;
    private TreeNode left=null;
    private TreeNode right=null;
    private TreeNode parent=null;

    public TreeNode(UUID encryptedId, Data data) {
        this.encryptedId = encryptedId;
        this.data = data;
    }

    public TreeNode(UUID encryptedId, Data data, TreeNode parent) {
        this.encryptedId = encryptedId;
        this.data = data;
        this.parent = parent;
    }

    public TreeNode(UUID encryptedId, Data data, TreeNode left, TreeNode right, TreeNode parent) {
        this.encryptedId = encryptedId;
        this.data = data;
        this.left = left;
        this.right = right;
        this.parent = parent;
    }

    public UUID getEncryptedId() {
        return encryptedId;
    }

    public Data getData() {
        return data;
    }

    public TreeNode getLeft() {
        return left;
    }

    public TreeNode getRight() {
        return right;
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setEncryptedId(UUID encryptedId) {
        this.encryptedId = encryptedId;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public void setLeft(TreeNode left) {
        this.left = left;
    }

    public void setRight(TreeNode right) {
        this.right = right;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }
}
