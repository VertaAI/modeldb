package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TreeModelDao {
  public static class Tree {
    private final Map<Integer, Pair<Integer, Integer>> leftRightIndicesForIndex;
    private final List<Integer> parentForIndex;
    private final TreeComponent component;

    public Tree(TreeComponent component) throws InvalidFieldException {
      this.component = component;
      leftRightIndicesForIndex = new HashMap<>();
      parentForIndex = new ArrayList<>();

      // Initialize the structures above.
      int numNodes = component.nodes.size();
      IntStream.range(0, numNodes).forEach(i -> {
        leftRightIndicesForIndex.put(i, new Pair<>(null, null));
        parentForIndex.add(null);
      });

      // Process the links. We use a for-loop because this code can throw an exception.
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
          leftRightIndicesForIndex.get(link.parentIndex).getKey() :
          leftRightIndicesForIndex.get(link.parentIndex).getValue();
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
          new Pair<>(link.childIndex, oldPair.getValue()) :
          new Pair<>(oldPair.getKey(), link.childIndex);
        leftRightIndicesForIndex.put(link.parentIndex, newPair);
      }

      // At the end, verify that there's a single root node.
      if (parentForIndex.stream().filter(i -> i == null).count() != 1) {
        throw new InvalidFieldException("Invalid tree component - must have exactly one root node");
      }
    }

    public boolean hasLeftChild(int i) {
      return leftRightIndicesForIndex.get(i).getKey() != null;
    }

    public boolean hasRightChild(int i) {
      return leftRightIndicesForIndex.get(i).getValue() != null;
    }

    public int leftChild(int i) {
      return leftRightIndicesForIndex.get(i).getKey();
    }

    public int rightChild(int i) {
      return leftRightIndicesForIndex.get(i).getValue();
    }

    public int rootNode() {
      return IntStream
        .range(0, parentForIndex.size())
        .filter(i -> parentForIndex.get(i) == null)
        .boxed()
        .collect(Collectors.toList())
        .get(0);
    }

    public boolean hasParent(int i) {
      return parentForIndex.get(i) != null;
    }

    public int parent(int i) {
      return parentForIndex.get(i);
    }

    public boolean isLeaf(int i) {
      return !hasLeftChild(i) && !hasRightChild(i);
    }

    public TreeNode get(int i) {
      return component.nodes.get(i);
    }
  }

  public static int storeNode(TreeNode treeNode, Integer parentId, Integer rootId, boolean isLeaf, DSLContext ctx) {
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

    // Store the link from this node to parent.
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

  public static int storeComponent(int modelId, int componentIndex, TreeComponent component, DSLContext ctx) throws InvalidFieldException {
    Tree tree = new Tree(component);

    // Create a stack to process.
    Integer rootId = null;
    Stack<Pair<Integer, Integer>> toProcess = new Stack<>();
    toProcess.push(new Pair<>(null, tree.rootNode()));

    // Store each node.
    TreeNode processNode;
    int processIndex;
    int processId;
    Pair<Integer, Integer> parentChildPair;
    while (!toProcess.empty()) {
      // Get the index and node of the next item in the stack.
      parentChildPair = toProcess.pop();
      processIndex = parentChildPair.getValue();
      processNode = tree.get(processIndex);

      // Store the node.
      processId = storeNode(processNode, parentChildPair.getKey(), rootId, tree.isLeaf(processIndex), ctx);

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

    // Now that all the TreeNodes and TreeLinks have been stored. store the TreeModelComponent.
    TreemodelcomponentRecord rec = ctx.newRecord(Tables.TREEMODELCOMPONENT);
    rec.setId(null);
    rec.setModel(modelId);
    rec.setComponentweight(component.weight);
    rec.setComponentindex(componentIndex);
    rec.setRootnode(rootId);
    rec.store();

    return rec.getId();
  }
  
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

    // Store each component.
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
    return false;
  }
}
