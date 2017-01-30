package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class contains logic for storing and reading tree models in the database.
 *
 * Tree models include Decision Trees, Random Forests, and Gradient Boosted Trees.
 */
public class TreeModelDao {
  /**
   * This class represents a single decision tree.
   */
  public static class Tree {
    // This maps from a TreeNode ID to its (left child TreeNode ID, right child TreeNode ID) pair. Note that
    // an entry in the pair could be null if there is no child.
    private final Map<Integer, Pair<Integer, Integer>> leftRightIndicesForIndex;

    // This maps from a TreeNode ID to the ID of its parent. The null value means no parent exists.
    private final List<Integer> parentForIndex;

    // This is the TreeComponent that was used to construct this Tree object.
    private final TreeComponent component;

    public Tree(TreeComponent component) throws InvalidFieldException {
      this.component = component;
      leftRightIndicesForIndex = new HashMap<>();
      parentForIndex = new ArrayList<>();

      // Initialize the structures above.
      int numNodes = component.nodes.size();

      // We'll start off by assuming none of the nodes have children and none of the nodes have parents.
      IntStream.range(0, numNodes).forEach(i -> {
        leftRightIndicesForIndex.put(i, new Pair<>(null, null));
        parentForIndex.add(null);
      });

      // Process the links. We use a for-loop because this code can throw an exception. Using a forEach would require
      // that the loop body not throw any exceptions.
      int numLinks = component.links.size();
      TreeLink link;
      for (int i = 0; i < numLinks; i++) {
        link = component.links.get(i);

        // Verify that the indices are valid.
        if (link.childIndex < 0 ||
          link.childIndex >= numNodes ||
          link.parentIndex < 0 ||
          link.parentIndex >= numNodes) {
          throw new InvalidFieldException(String.format(
            "Invalid tree component - Link %d -> %d must have indices between 0 and %d",
            link.parentIndex,
            link.childIndex,
            numNodes
          ));
        }

        // Verify that this child does not already have a parent.
        if (parentForIndex.get(link.childIndex) != null && parentForIndex.get(link.childIndex) != link.parentIndex) {
          throw new InvalidFieldException(String.format(
            "Invalid tree component - child %d has two parents, %d and %d",
            link.childIndex,
            link.parentIndex,
            parentForIndex.get(link.childIndex)
          ));
        }

        // Verify that the parent does not already have this child.
        Integer childInd = link.isLeft ?
          leftRightIndicesForIndex.get(link.parentIndex).getFirst() :
          leftRightIndicesForIndex.get(link.parentIndex).getSecond();
        if (childInd != null && childInd != link.childIndex) {
          throw new InvalidFieldException(String.format(
            "Invalid tree component - parent %d has two %s children, %d and %d",
            link.parentIndex,
            link.isLeft ? "left" : "right",
            childInd,
            link.childIndex
          ));
        }

        // Store the parent for the child.
        parentForIndex.set(link.childIndex, link.parentIndex);

        // Store the child for the parent.
        Pair<Integer, Integer> oldPair = leftRightIndicesForIndex.get(link.parentIndex);
        Pair<Integer, Integer> newPair = link.isLeft ?
          new Pair<>(link.childIndex, oldPair.getSecond()) :
          new Pair<>(oldPair.getFirst(), link.childIndex);
        leftRightIndicesForIndex.put(link.parentIndex, newPair);
      }

      // At the end, verify that there's a single root node.
      if (parentForIndex.stream().filter(i -> i == null).count() != 1) {
        throw new InvalidFieldException("Invalid tree component - must have exactly one root node");
      }
    }

    /**
     * @param i - The ID of a node.
     * @return Whether the node has a left child.
     */
    public boolean hasLeftChild(int i) {
      return leftRightIndicesForIndex.get(i).getFirst() != null;
    }

    /**
     * @param i - The ID of a node.
     * @return Whether the node has a right child.
     */
    public boolean hasRightChild(int i) {
      return leftRightIndicesForIndex.get(i).getSecond() != null;
    }

    /**
     * @param i - The ID of a node. The node MUST have a left child.
     * @return The ID of the node's left child.
     */
    public int leftChild(int i) {
      return leftRightIndicesForIndex.get(i).getFirst();
    }

    /**
     * @param i - The ID of a node. The node MUST have a right child.
     * @return The ID of the node's right child.
     */
    public int rightChild(int i) {
      return leftRightIndicesForIndex.get(i).getSecond();
    }

    /**
     * @return The ID of the root node of this tree.
     */
    public int rootNode() {
      return IntStream
        .range(0, parentForIndex.size())
        .filter(i -> parentForIndex.get(i) == null)
        .boxed()
        .collect(Collectors.toList())
        .get(0);
    }

    /**
     * @param i - The ID of a node.
     * @return Whether the node has a parent.
     */
    public boolean hasParent(int i) {
      return parentForIndex.get(i) != null;
    }

    /**
     * @param i - The ID of a node. The node MUSt have a parent.
     * @return THe ID of the node's parent.
     */
    public int parent(int i) {
      return parentForIndex.get(i);
    }

    /**
     * @param i - The ID of a node.
     * @return Whether the node is a leaf node.
     */
    public boolean isLeaf(int i) {
      return !hasLeftChild(i) && !hasRightChild(i);
    }

    /**
     * @param i - The ID of a node.
     * @return The TreeNode with the given ID.
     */
    public TreeNode get(int i) {
      return component.nodes.get(i);
    }
  }

  /**
   * Store an entry in the TreeNode and TreeLink tables to reflect a single node.
   * @param treeNode - The node to store.
   * @param parentId - The ID of the parent of the given node. This should be null if the node has no parent.
   * @param rootId - The ID of the root node in the tree that contains the given node. This should be null if the given
   *               node is the root node.
   * @param isLeaf - Whether this node is a leaf node in its tree.
   * @param ctx - The database context.
   * @return The ID of the given node after it has been stored in the database.
   */
  public static int storeNode(TreeNode treeNode, Integer parentId, Integer rootId, boolean isLeaf, DSLContext ctx) {
    // Store an entry in the TreeNode table.
    TreenodeRecord rec = ctx.newRecord(Tables.TREENODE);
    int isLeftInt = isLeaf ? 1 : 0;
    rec.setId(null);
    rec.setIsleaf(isLeftInt);
    rec.setPrediction(treeNode.prediction);
    rec.setImpurity(treeNode.impurity);
    if (treeNode.isSetGain()) {
      rec.setGain(treeNode.gain);
    }
    if (treeNode.isSetSplitIndex()) {
      rec.setSplitindex(treeNode.splitIndex);
    }
    if (rootId != null) {
      rec.setRootnode(rootId);
    }
    rec.store();

    int nodeId = rec.getId();

    // If there's a parent, store an entry in the TreeLink table connecting the given node to its parent.
    if (parentId != null) {
      TreelinkRecord linkRec = ctx.newRecord(Tables.TREELINK);
      linkRec.setId(null);
      linkRec.setParent(parentId);
      linkRec.setChild(nodeId);
      linkRec.setIsleft(isLeftInt);
      linkRec.store();
      linkRec.getId();
    }

    return nodeId;
  }

  /**
   * Store a TreeComponent (i.e. a decision tree) in the database.
   * @param modelId - The underlying model ID (i.e. ID of a Transformer in the Transformer table) that represents
   *                this TreeComponent. There MUST be a Transformer in the Transformer table with this ID.
   * @param componentIndex - The index of this component in its overall TreeModel. A random forest model has multiple
   *                       decision trees, for instance, and each one of these decision trees would be considered a
   *                       a TreeComponent (each with its own index). A decision tree model could be
   *                       considered as a single TreeComponent (index = 0).
   * @param component - The actual tree component to store.
   * @param ctx - The database context.
   * @return The ID of the TreeComponent after it has been stored in the TreeComponent table.
   * @throws InvalidFieldException - Thrown if any of the fields in the TreeComponent are not properly defined.
   */
  public static int storeComponent(int modelId, int componentIndex, TreeComponent component, DSLContext ctx)
    throws InvalidFieldException {
    // Parse the component into a Tree object. This will make the tree traversal more efficient.
    Tree tree = new Tree(component);

    // Create a stack to process the nodes one at a time.
    Integer rootId = null;
    Stack<Pair<Integer, Integer>> toProcess = new Stack<>();
    toProcess.push(new Pair<>(null, tree.rootNode()));

    // Store each node.
    TreeNode processNode;
    int processIndex;
    int processId;
    Pair<Integer, Integer> parentChildPair;

    // Keep looping until all the nodes have been processed.
    while (!toProcess.empty()) {
      // Get the index and node of the next item in the stack.
      parentChildPair = toProcess.pop();
      processIndex = parentChildPair.getSecond();
      processNode = tree.get(processIndex);

      // Store the node.
      processId = storeNode(processNode, parentChildPair.getFirst(), rootId, tree.isLeaf(processIndex), ctx);

      // Set the root if necessary.
      if (rootId == null) {
        rootId = processId;
      }

      // Push the children on the stack.
      if (tree.hasLeftChild(processIndex)) {
        toProcess.push(new Pair<>(processId, tree.leftChild(processIndex)));
      }
      if (tree.hasRightChild(processIndex)) {
        toProcess.push(new Pair<>(processId, tree.rightChild(processIndex)));
      }
    }

    // Now that all the TreeNodes and TreeLinks have been stored. store an entry in TreeModelComponent.
    TreemodelcomponentRecord rec = ctx.newRecord(Tables.TREEMODELCOMPONENT);
    rec.setId(null);
    rec.setModel(modelId);
    rec.setComponentweight(component.weight);
    rec.setComponentindex(componentIndex);
    rec.setRootnode(rootId);
    rec.store();

    return rec.getId();
  }

  /**
   * Store a TreeModel in the database.
   * @param modelId - The ID (i.e. primary key) of the underlying Transformer that reflects this TreeModel.
   * @param model - The TreeModel.
   * @param ctx - The database context.
   * @return Whether the TreeModel was stored successfully.
   * @throws ResourceNotFoundException - Thrown if there's no entry in the Transformer table with the given modelId.
   * @throws InvalidFieldException - Thrown if any of the fields of the TreeModel are invalid.
   */
  public static boolean store(int modelId, TreeModel model, DSLContext ctx)
    throws ResourceNotFoundException, InvalidFieldException {
    // Check if the modelId exists.
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(
        String.format("Cannot store tree model for Transformer %d because it doesn't exist", modelId)
      );
    }

    // Store the TreeModel.
    TreemodelRecord tmRec = ctx.newRecord(Tables.TREEMODEL);
    tmRec.setId(null);
    tmRec.setModeltype(model.modelType);
    tmRec.setModel(modelId);
    tmRec.store();
    tmRec.getId();

    // Store each component. We use a for-loop rather than a forEach because the storeComponent method can throw
    // an exception.
    for (int i = 0; i < model.components.size(); i++) {
      storeComponent(modelId, i, model.components.get(i), ctx);
    }

    // Get the features for the model.
    List<FeatureRecord> features = ctx.selectFrom(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId))
      .fetch()
      .stream()
      .collect(Collectors.toList());

    // Delete all the features for the models.
    ctx.deleteFrom(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId));

    // Store the features again.
    features.forEach(ft -> {
      ft.setImportance(Math.abs(model.featureImportances.get(ft.getFeatureindex())));
      ft.store();
      ft.getImportance();
    });

    // TODO: Do we need a return value for this method? It's always true.
    return true;
  }
}
