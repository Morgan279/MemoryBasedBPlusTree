package xyz.proadap.aliang;

import java.util.*;

public class BPlusTree<K extends Comparable<K>, E> {

    private final int OVERFLOW_BOUND;

    private final int UNDERFLOW_BOUND;

    private BPlusTreeNode root;

    public BPlusTree(int order) {
        if(order < 3){
            throw new IllegalArgumentException("The order of BPlus Tree must be greater than or equal to 3");
        }
        this.OVERFLOW_BOUND = order - 1;
        this.UNDERFLOW_BOUND = OVERFLOW_BOUND / 2;
    }

    public BPlusTree() {
        this.OVERFLOW_BOUND = 8;
        this.UNDERFLOW_BOUND = OVERFLOW_BOUND / 2;
    }

    public void insert(K entry, E value) {

        if (root == null) {
            root = new BPlusTreeLeafNode(asList(entry), asList(asSet(value)));
            return;
        }


        BPlusTreeNode newChildNode = root.insert(entry, value);
        if (newChildNode != null) {
            K newRootEntry = newChildNode.getClass().equals(BPlusTreeLeafNode.class)
                    ? newChildNode.entries.get(0)
                    : ((BPlusTreeNonLeafNode) newChildNode).findLeafEntry(newChildNode);
            root = new BPlusTreeNonLeafNode(asList(newRootEntry), asList(root, newChildNode));
        }

    }

    public List<E> query(K entry) {
        if (root == null) {
            return Collections.emptyList();
        }
        return root.query(entry);
    }

    public List<E> rangeQuery(K startInclude, K endExclude) {
        if (root == null) {
            return Collections.emptyList();
        }
        return root.rangeQuery(startInclude, endExclude);
    }

    public boolean update(K entry, E oldValue, E newValue) {
        if (root == null) {
            return false;
        }

        return root.update(entry, oldValue, newValue);
    }

    public boolean remove(K entry, E value) {
        if (root == null) {
            return false;
        }

        RemoveResult removeResult = root.remove(entry, value);
        if (!removeResult.isRemoved) {
            return false;
        }

        if (root.entries.isEmpty()) {
            this.handleRootUnderflow();
        }

        return true;
    }

    public boolean remove(K entry) {
        if (root == null) {
            return false;
        }

        RemoveResult removeResult = root.remove(entry);
        if (!removeResult.isRemoved) {
            return false;
        }

        if (root.entries.isEmpty()) {
            this.handleRootUnderflow();
        }

        return true;
    }

    private void handleRootUnderflow() {
        root = root.getClass().equals(BPlusTreeLeafNode.class) ? null : ((BPlusTreeNonLeafNode) root).children.get(0);
    }

    @SafeVarargs
    private final <T> List<T> asList(T... e) {
        List<T> res = new ArrayList<>();
        Collections.addAll(res, e);
        return res;
    }

    @SafeVarargs
    private final <T> Set<T> asSet(T... e) {
        Set<T> res = new HashSet<>();
        Collections.addAll(res, e);
        return res;
    }

    @Override
    public String toString() {
        if(root == null){
            return "";
        }
        return root.toString();
    }

    private abstract class BPlusTreeNode {

        protected List<K> entries;

        protected boolean isUnderflow() {
            return entries.size() < UNDERFLOW_BOUND;
        }

        protected boolean isOverflow(){ return entries.size() > OVERFLOW_BOUND; }

        protected int getMedianIndex() {
            return OVERFLOW_BOUND / 2;
        }

        protected int entryIndexUpperBound(K entry) {
            int l = 0;
            int r = entries.size();
            while (l < r) {
                int mid = l + ((r - l) >> 1);
                if (entries.get(mid).compareTo(entry) <= 0) {
                    l = mid + 1;
                } else {
                    r = mid;
                }
            }
            return l;
        }

        public abstract List<E> rangeQuery(K startInclude, K endExclude);

        public abstract List<E> query(K entry);

        public abstract BPlusTreeNode insert(K entry, E value);

        public abstract boolean update(K entry, E oldValue, E newValue);

        public abstract RemoveResult remove(K entry);

        public abstract RemoveResult remove(K entry, E value);

        public abstract void combine(BPlusTreeNode neighbor, K parentEntry);

        public abstract void borrow(BPlusTreeNode neighbor, K parentEntry, boolean isLeft);
    }

    private class BPlusTreeNonLeafNode extends BPlusTreeNode {

        public List<BPlusTreeNode> children;

        public BPlusTreeNonLeafNode(List<K> entries, List<BPlusTreeNode> children) {
            this.entries = entries;
            this.children = children;
        }


        @Override
        public List<E> rangeQuery(K startInclude, K endExclude) {
            return children.get(entryIndexUpperBound(startInclude)).rangeQuery(startInclude, endExclude);
        }

        @Override
        public List<E> query(K entry) {
            return children.get(entryIndexUpperBound(entry)).query(entry);
        }

        @Override
        public boolean update(K entry, E oldValue, E newValue) {
            return children.get(entryIndexUpperBound(entry)).update(entry, oldValue, newValue);
        }

        @Override
        public BPlusTreeNode insert(K entry, E value) {
            BPlusTreeNode newChildNode = children.get(entryIndexUpperBound(entry)).insert(entry, value);

            if (newChildNode != null) {
                K newEntry = findLeafEntry(newChildNode);
                int newEntryIndex = entryIndexUpperBound(newEntry);
                entries.add(newEntryIndex, newEntry);
                children.add(newEntryIndex + 1, newChildNode);
                return isOverflow() ? split() : null;
            }

            return null;
        }

        @Override
        public RemoveResult remove(K entry) {
            int childIndex = entryIndexUpperBound(entry);
            int entryIndex = Math.max(0, childIndex - 1);
            BPlusTreeNode childNode = children.get(childIndex);
            RemoveResult removeResult = childNode.remove(entry);
            if (!removeResult.isRemoved) {
                return removeResult;
            }

            if (removeResult.isUnderflow) {
                this.handleUnderflow(childNode, childIndex, entryIndex);
            }

            return new RemoveResult(true, isUnderflow());
        }

        @Override
        public RemoveResult remove(K entry, E value) {
            int childIndex = entryIndexUpperBound(entry);
            int entryIndex = Math.max(0, childIndex - 1);

            BPlusTreeNode childNode = children.get(childIndex);
            RemoveResult removeResult = childNode.remove(entry, value);
            if (!removeResult.isRemoved) {
                return removeResult;
            }

            if (removeResult.isUnderflow) {
                this.handleUnderflow(childNode, childIndex, entryIndex);
            }

            return new RemoveResult(true, isUnderflow());
        }


        private void handleUnderflow(BPlusTreeNode childNode, int childIndex, int entryIndex) {
            BPlusTreeNode neighbor;
            if (childIndex > 0 && (neighbor = this.children.get(childIndex - 1)).entries.size() > UNDERFLOW_BOUND) {

                childNode.borrow(neighbor, this.entries.get(entryIndex), true);
                K boundEntry = childNode.getClass().equals(BPlusTreeNonLeafNode.class) ? findLeafEntry(childNode) : childNode.entries.get(0);
                this.entries.set(entryIndex, boundEntry);

            } else if (childIndex < this.children.size() - 1 && (neighbor = this.children.get(childIndex + 1)).entries.size() > UNDERFLOW_BOUND) {

                int parentEntryIndex = childIndex == 0 ? 0 :Math.min(this.entries.size() - 1, entryIndex + 1);
                childNode.borrow(neighbor, this.entries.get(parentEntryIndex), false);
                this.entries.set(parentEntryIndex, childNode.getClass().equals(BPlusTreeNonLeafNode.class) ? findLeafEntry(neighbor) : neighbor.entries.get(0));

            } else {

                if (childIndex > 0) {
                    // combine current child to left child
                    neighbor = this.children.get(childIndex - 1);
                    neighbor.combine(childNode, this.entries.get(entryIndex));
                    this.entries.remove(entryIndex);
                    this.children.remove(childIndex);

                } else {
                    // combine right child to current child (child index = 0)
                    neighbor = this.children.get(1);
                    childNode.combine(neighbor, this.entries.get(0));
                    this.entries.remove(0);
                    this.children.remove(1);
                }

            }

        }

        private BPlusTreeNonLeafNode split() {
            int medianIndex = getMedianIndex();
            List<K> allEntries = entries;
            List<BPlusTreeNode> allChildren = children;

            this.entries = new ArrayList<>(allEntries.subList(0, medianIndex));
            this.children = new ArrayList<>(allChildren.subList(0, medianIndex + 1));

            List<K> rightEntries = new ArrayList<>(allEntries.subList(medianIndex + 1, allEntries.size()));
            List<BPlusTreeNode> rightChildren = new ArrayList<>(allChildren.subList(medianIndex + 1, allChildren.size()));
            return new BPlusTreeNonLeafNode(rightEntries, rightChildren);
        }

        @Override
        public void combine(BPlusTreeNode neighbor, K parentEntry) {
            BPlusTreeNonLeafNode nonLeafNode = (BPlusTreeNonLeafNode) neighbor;
            this.entries.add(parentEntry);
            this.entries.addAll(nonLeafNode.entries);
            this.children.addAll(nonLeafNode.children);
        }

        @Override
        public void borrow(BPlusTreeNode neighbor, K parentEntry, boolean isLeft) {
            BPlusTreeNonLeafNode nonLeafNode = (BPlusTreeNonLeafNode) neighbor;
            if (isLeft) {
                this.entries.add(0, parentEntry);
                this.children.add(0, nonLeafNode.children.get(nonLeafNode.children.size() - 1));
                nonLeafNode.children.remove(nonLeafNode.children.size() - 1);
                nonLeafNode.entries.remove(nonLeafNode.entries.size() - 1);
            } else {
                this.entries.add(parentEntry);
                this.children.add(nonLeafNode.children.get(0));
                nonLeafNode.entries.remove(0);
                nonLeafNode.children.remove(0);
            }
        }

        public K findLeafEntry(BPlusTreeNode cur) {
            if (cur.getClass().equals(BPlusTreeLeafNode.class)) {
                return cur.entries.get(0);
            }
            return findLeafEntry(((BPlusTreeNonLeafNode) cur).children.get(0));
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            Queue<BPlusTreeNode> queue = new LinkedList<>();
            queue.add(this);
            while (!queue.isEmpty()) {
                int size = queue.size();
                for (int i = 0; i < size; ++i) {
                    BPlusTreeNode cur = queue.poll();
                    assert cur != null;
                    res.append(cur.entries).append("  ");
                    if (cur.getClass().equals(BPlusTreeNonLeafNode.class)) {
                        queue.addAll(((BPlusTreeNonLeafNode) cur).children);
                    }
                }
                res.append('\n');
            }
            return res.toString();
        }
    }

    private class BPlusTreeLeafNode extends BPlusTreeNode {

        public List<Set<E>> data;

        public BPlusTreeLeafNode next;

        public BPlusTreeLeafNode(List<K> entries, List<Set<E>> data) {
            this.entries = entries;
            this.data = data;
        }

        @Override
        public List<E> rangeQuery(K startInclude, K endExclude) {
            List<E> res = new ArrayList<>();
            int startUpperBound = entryIndexUpperBound(startInclude);
            if(startUpperBound == 0){
                return Collections.emptyList();
            }

            int end = entryIndexUpperBound(endExclude) - 1;
            if(end >= 0 && entries.get(end) == endExclude){
                --end;
            }

            for (int i = startUpperBound - 1; i <= end; ++i) {
                res.addAll(data.get(i));
            }

            BPlusTreeLeafNode nextLeafNode = next;
            while (nextLeafNode != null) {
                for (int i = 0; i < nextLeafNode.entries.size(); ++i) {
                    if (nextLeafNode.entries.get(i).compareTo(endExclude) < 0) {
                        res.addAll(nextLeafNode.data.get(i));
                    } else {
                        return res;
                    }
                }
                nextLeafNode = nextLeafNode.next;
            }
            return res;
        }

        @Override
        public List<E> query(K entry) {
            int entryIndex = getEqualEntryIndex(entry);
            return entryIndex == -1 ? Collections.emptyList() : new ArrayList<>(data.get(entryIndex));
        }

        @Override
        public boolean update(K entry, E oldValue, E newValue) {
            int entryIndex = getEqualEntryIndex(entry);
            if (entryIndex == -1 || !data.get(entryIndex).contains(oldValue)) {
                return false;
            }

            data.get(entryIndex).remove(oldValue);
            data.get(entryIndex).add(newValue);
            return true;
        }

        @Override
        public RemoveResult remove(K entry) {
            int entryIndex = getEqualEntryIndex(entry);
            if (entryIndex == -1) {
                return new RemoveResult(false, false);
            }

            this.entries.remove(entryIndex);
            this.data.remove(entryIndex);

            return new RemoveResult(true, isUnderflow());
        }

        @Override
        public RemoveResult remove(K entry, E value) {
            int entryIndex = getEqualEntryIndex(entry);
            if (entryIndex == -1 || !data.get(entryIndex).contains(value)) {
                return new RemoveResult(false, false);
            }

            data.get(entryIndex).remove(value);
            if (data.get(entryIndex).isEmpty()) {
                this.entries.remove(entryIndex);
                this.data.remove(entryIndex);
            }

            return new RemoveResult(true, isUnderflow());
        }

        @Override
        public void combine(BPlusTreeNode neighbor, K parentEntry) {
            BPlusTreeLeafNode leafNode = (BPlusTreeLeafNode) neighbor;
            this.entries.addAll(leafNode.entries);
            this.data.addAll(leafNode.data);
            this.next = leafNode.next;
        }

        @Override
        public void borrow(BPlusTreeNode neighbor, K parentEntry, boolean isLeft) {
            BPlusTreeLeafNode leafNode = (BPlusTreeLeafNode) neighbor;
            int borrowIndex;

            if (isLeft) {
                borrowIndex = leafNode.entries.size() - 1;
                this.entries.add(0, leafNode.entries.get(borrowIndex));
                this.data.add(0, leafNode.data.get(borrowIndex));
            } else {
                borrowIndex = 0;
                this.entries.add(leafNode.entries.get(borrowIndex));
                this.data.add(leafNode.data.get(borrowIndex));
            }

            leafNode.entries.remove(borrowIndex);
            leafNode.data.remove(borrowIndex);
        }

        @Override
        public BPlusTreeNode insert(K entry, E value) {
            int equalEntryIndex = getEqualEntryIndex(entry);
            if (equalEntryIndex != -1) {
                data.get(equalEntryIndex).add(value);
                return null;
            }

            int index = entryIndexUpperBound(entry);
            entries.add(index, entry);
            data.add(index, asSet(value));
            return isOverflow() ? split() : null;
        }

        private BPlusTreeLeafNode split() {
            int medianIndex = getMedianIndex();
            List<K> allEntries = entries;
            List<Set<E>> allData = data;

            this.entries = new ArrayList<>(allEntries.subList(0, medianIndex));
            this.data = new ArrayList<>(allData.subList(0, medianIndex));

            List<K> rightEntries = new ArrayList<>(allEntries.subList(medianIndex, allEntries.size()));
            List<Set<E>> rightData = new ArrayList<>(allData.subList(medianIndex, allData.size()));
            BPlusTreeLeafNode newLeafNode = new BPlusTreeLeafNode(rightEntries, rightData);

            newLeafNode.next = this.next;
            this.next = newLeafNode;
            return newLeafNode;
        }

        private int getEqualEntryIndex(K entry) {
            int l = 0;
            int r = entries.size() - 1;
            while (l <= r) {
                int mid = l + ((r - l) >> 1);
                int compare = entries.get(mid).compareTo(entry);
                if (compare == 0) {
                    return mid;
                } else if (compare > 0) {
                    r = mid - 1;
                } else {
                    l = mid + 1;
                }
            }
            return -1;
        }

        @Override
        public String toString() {
            return entries.toString();
        }
    }

    private static class RemoveResult {

        public boolean isRemoved;

        public boolean isUnderflow;

        public RemoveResult(boolean isRemoved, boolean isUnderflow) {
            this.isRemoved = isRemoved;
            this.isUnderflow = isUnderflow;
        }
    }
}
