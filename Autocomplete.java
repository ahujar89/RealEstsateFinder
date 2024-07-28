import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class AVLNode {
    // Node declaration
    String word;
    int frequency;
    int height;
    AVLNode left, right;

    AVLNode(String word) {
        this.word = word;
        this.frequency = 1;
        this.height = 1;
    }
}

class AVLTree {
    // AVL Tree Methods
    private AVLNode root;

    private int height(AVLNode N) {
        if (N == null) return 0;
        return N.height;
    }

    private AVLNode rightRotate(AVLNode y) {
        if (y == null) return null;
        AVLNode x = y.left;
        AVLNode T2 = x.right;

        x.right = y;
        y.left = T2;

        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        return x;
    }

    private AVLNode leftRotate(AVLNode x) {
        if (x == null) return null;
        AVLNode y = x.right;
        AVLNode T2 = y.left;

        y.left = x;
        x.right = T2;

        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        return y;
    }

    private int getBalance(AVLNode N) {
        if (N == null) return 0;
        return height(N.left) - height(N.right);
    }

    public void insert(String word) {
        root = insertRec(root, word);
    }

    private AVLNode insertRec(AVLNode node, String word) {
        if (node == null) {
            return new AVLNode(word);
        }

        if (word.compareTo(node.word) < 0) {
            node.left = insertRec(node.left, word);
        } else if (word.compareTo(node.word) > 0) {
            node.right = insertRec(node.right, word);
        } else {
            node.frequency++;
            return node;
        }

        node.height = 1 + Math.max(height(node.left), height(node.right));
        int balance = getBalance(node);

        if (balance > 1 && word.compareTo(node.left.word) < 0) {
            return rightRotate(node);
        }

        if (balance < -1 && word.compareTo(node.right.word) > 0) {
            return leftRotate(node);
        }

        if (balance > 1 && word.compareTo(node.left.word) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        if (balance < -1 && word.compareTo(node.right.word) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    public List<String> findWordsWithPrefix(String prefix) {
        List<String> result = new ArrayList<>();
        findWordsWithPrefixRec(root, prefix, result);
        return result;
    }

    private void findWordsWithPrefixRec(AVLNode node, String prefix, List<String> result) {
        if (node == null) return;

        if (prefix.compareTo(node.word) <= 0) {
            findWordsWithPrefixRec(node.left, prefix, result);
        }

        if (node.word.startsWith(prefix)) {
            result.add(node.word + " (" + node.frequency + ")");
        }

        if (prefix.compareTo(node.word) <= 0 || prefix.compareTo(node.word) > 0) {
            findWordsWithPrefixRec(node.right, prefix, result);
        }
    }
}
// Suggestions class
class Autocomplete {
    private AVLTree avlTree;

    public Autocomplete() {
        avlTree = new AVLTree();
    }

    public void buildVocabularyFromRemaxFile(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            boolean isHeader = true;
            while ((nextLine = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String city = nextLine[2]; // Assuming city names are in the second column
                avlTree.insert(city.toLowerCase());
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Error reading CSV file: " + e.getMessage());
        }
    }

    public List<String> getSuggestions(String prefix) {
        prefix = prefix.toLowerCase();
        List<String> wordsWithPrefix = avlTree.findWordsWithPrefix(prefix);

        PriorityQueue<String> minHeap = new PriorityQueue<>((a, b) -> {
            int freqA = Integer.parseInt(a.substring(a.lastIndexOf('(') + 1, a.lastIndexOf(')')));
            int freqB = Integer.parseInt(b.substring(b.lastIndexOf('(') + 1, b.lastIndexOf(')')));
            return Integer.compare(freqA, freqB);
        });

        for (String word : wordsWithPrefix) {
            minHeap.offer(word);
            if (minHeap.size() > 5) {
                minHeap.poll();
            }
        }

        List<String> topSuggestions = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            topSuggestions.add(minHeap.poll());
        }

        Collections.sort(topSuggestions, (a, b) -> {
            int freqA = Integer.parseInt(a.substring(a.lastIndexOf('(') + 1, a.lastIndexOf(')')));
            int freqB = Integer.parseInt(b.substring(b.lastIndexOf('(') + 1, b.lastIndexOf(')')));
            int freqComparison = Integer.compare(freqB, freqA);
            if (freqComparison != 0) {
                return freqComparison;
            }
            return a.compareTo(b);
        });

        return topSuggestions;
    }
}
