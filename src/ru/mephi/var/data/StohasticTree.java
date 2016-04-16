package ru.mephi.var.data;

import gost28147.Gost28147Static;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by Sid_Hugo on 16.04.2016.
 */
public class StohasticTree {
    TreeNode root;

    public StohasticTree(TreeNode root) {
        this.root = root;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public boolean put(UUID uuid, Data data) {
        UUID encryptedUuid = encryptId(uuid);
        if(!containsEncryptedId(encryptedUuid)) {
            Data dataForStore = encryptData(data);
            putIntoTree(encryptedUuid, dataForStore);
            return true;
        }
        return false;
    }

    public boolean delete(UUID uuid) {
        UUID encryptedUuid=encryptId(uuid);
        if(containsEncryptedId(encryptedUuid)) {
            removeFromTree(encryptedUuid);
            return true;
        }
        return false;
    }

    private void removeFromTree(UUID encryptedId) {
        if(root==null) {
            return;
        }
        TreeNode currentNode=root;
        while (true) {
            if(currentNode==null)
                return;
            switch (currentNode.getEncryptedId().compareTo(encryptedId)) {
                case -1:
                    currentNode=currentNode.getRight();
                    break;
                case 0:
                    // TODO: to consider three cases of this node
                    TreeNode left=currentNode.getLeft();
                    TreeNode right=currentNode.getRight();
                    TreeNode parent=currentNode.getParent();
                    if(left==null && right==null) {
                        if(parent.getLeft()==currentNode)
                            parent.setLeft(null);
                        else
                            parent.setRight(null);
                    } else if(left==null) {
                        right.setParent(parent);
                        if(parent.getLeft()==currentNode)
                            parent.setLeft(right);
                        else
                            parent.setRight(right);
                    } else if(right==null) {
                        left.setParent(parent);
                        if(parent.getLeft()==currentNode)
                            parent.setLeft(left);
                        else
                            parent.setRight(left);
                    } else {
                        TreeNode nextNode=right;
                        while (nextNode.getLeft()!=null) {
                            nextNode=nextNode.getLeft();
                        }
                        TreeNode nextNodeParent=nextNode.getParent();
                        if(nextNodeParent.getLeft()==nextNode)
                            nextNodeParent.setLeft(null);
                        else
                            nextNodeParent.setRight(null);
                        nextNode.setParent(parent);
                        nextNode.setLeft(currentNode.getLeft());
                        nextNode.setRight(currentNode.getRight());
                        if(parent.getLeft()==currentNode)
                            parent.setLeft(nextNode);
                        else
                            parent.setRight(nextNode);
                    }
                case 1:
                    currentNode=currentNode.getLeft();
            }
        }
    }

    // returns decrypted data
    public Data get(UUID uuid) {
        UUID encryptedId=encryptId(uuid);
        Data encryptedData= getDataByEncryptedId(encryptedId);
        byte[] decryptedData = Gost28147Static.gammaDecrypt(encryptedData.getData());
        return new Data(decryptedData, encryptedData.getType(), encryptedData.getLength());
    }

    private Data encryptData(Data data) {
        byte[] encryptedData;
        encryptedData = Gost28147Static.gammaEncrypt(data.getData());
        return new Data(encryptedData, data.getType(), data.getLength());
    }

    private UUID encryptId(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] arrId = bb.array();
        byte[] encryptedId= Gost28147Static.gammaEncrypt(arrId);
        bb = ByteBuffer.wrap(encryptedId);
        return new UUID(bb.getLong(), bb.getLong());
    }

    // with replacement
    private void putIntoTree(UUID encryptedId, Data dataForStore) {
        if(root==null) {
            root = new TreeNode(encryptedId, dataForStore);
            return;
        }
        TreeNode currentNode=root;
        while (true) {
            switch (currentNode.getEncryptedId().compareTo(encryptedId)) {
                case -1:
                    TreeNode right=currentNode.getRight();
                    if(right==null) {
                        currentNode.setRight(new TreeNode(encryptedId, dataForStore, currentNode));
                        return;
                    }
                    currentNode=currentNode.getRight();
                    break;
                case 0:
                    currentNode.setData(dataForStore);
                    return;
                case 1:
                    TreeNode left=currentNode.getLeft();
                    if(left==null) {
                        currentNode.setLeft(new TreeNode(encryptedId, dataForStore, currentNode));
                        return;
                    }
                    currentNode=currentNode.getLeft();
            }
        }
    }

    private boolean containsEncryptedId(UUID uuid) {
        return getDataByEncryptedId(uuid)==null ? false: true;
    }
    //returns encrypted data
    private Data getDataByEncryptedId(UUID encryptedId) {
        if(root==null)
            return null;
        TreeNode currentNode=root;
        while (true) {
            if(currentNode==null)
                return null;
            switch (currentNode.getEncryptedId().compareTo(encryptedId)) {
                case -1:
                    currentNode=currentNode.getRight();
                    break;
                case 0:
                    return currentNode.getData();
                case 1:
                    currentNode=currentNode.getLeft();
            }
        }
    }

    public boolean containsId(UUID uuid) {
        return get(uuid)==null ? false: true;
    }
}

