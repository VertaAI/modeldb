package edu.mit.csail.db.ml.modeldb.client

import modeldb.{TreeComponent, TreeLink, TreeModel, TreeNode}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, GBTClassificationModel, RandomForestClassificationModel}
import org.apache.spark.ml.regression.{DecisionTreeRegressionModel, GBTRegressionModel, RandomForestRegressionModel}
import org.apache.spark.ml.tree.{InternalNode, LeafNode, Node}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * This contains logic for converting a Spark tree model (e.g. DecisionTreeClassificationModel, GBTRegressionModel)
  * into a Thrift TreeModel.
  */
object SyncableTreeModel {
  /**
    * Recursively walk the tree at the given TreeNode and update the nodes, links, and indexForNode
    * structures.
    * @param node - A Spark Node (i.e. a Node in a decision tree).
    * @param nodes - An array of Thrift TreeNodes.
    * @param links - An array of Thrift TreeLinks.
    * @param indexForNode - Maps from a Thrift TreeNode to a number indicating its index in the list of TreeNodes.
    * @param parent - The parent of the node "node". If None, then the node "node" is taken to be the root of the tree.
    * @param isLeft - If the parent is not null, this indicates whether the node "node" is the left child of its parent.
    */
  private def updateState(node: Node,
                          nodes: ArrayBuffer[TreeNode],
                          links: ArrayBuffer[TreeLink],
                          indexForNode: mutable.HashMap[TreeNode, Int],
                          parent: Option[TreeNode],
                          isLeft: Boolean = true): Unit = node match {
    case intNode: InternalNode => {
      val n = TreeNode(intNode.prediction, intNode.impurity, Some(intNode.gain), Some(intNode.split.featureIndex))
      nodes.append(n)
      indexForNode.put(n, nodes.size - 1)
      if (parent.isDefined) {
        links.append(TreeLink(indexForNode(parent.get), indexForNode(n), isLeft))
      }
      if (intNode.leftChild != null) {
        updateState(intNode.leftChild, nodes, links, indexForNode, Some(n), isLeft = true)
      }
      if (intNode.rightChild != null) {
        updateState(intNode.rightChild, nodes, links, indexForNode, Some(n), isLeft = false)
      }
    }
    case leafNode: LeafNode => {
      val n = TreeNode(leafNode.prediction, leafNode.impurity)
      nodes.append(n)
      indexForNode.put(n, nodes.size - 1)
      if (parent.isDefined) {
        links.append(TreeLink(indexForNode(parent.get), indexForNode(n), isLeft))
      }
    }
  }

  /**
    * Create a Thrift TreeComponent from the tree rooted at the given Spark Node.
    * @param rootNode - The root node of the tree. This is a Spark Node.
    * @param weight - The weight of the resulting TreeComponent in the ensemble that it belongs
    *               to.
    * @return The Thrift TreeComponent.
    */
  private def toTreeComponent(rootNode: Node, weight: Double = 1.0): TreeComponent = {
    val indexForNode = mutable.HashMap[TreeNode, Int]()
    val nodes = ArrayBuffer[TreeNode]()
    val links = ArrayBuffer[TreeLink]()

    updateState(rootNode, nodes, links, indexForNode, None)
    TreeComponent(weight, nodes, links)
  }

  // The functions below convert various Spark tree models into Thrift TreeModels.
  private def toTreeModel(dt: DecisionTreeRegressionModel): TreeModel =
    TreeModel("Decision Tree", Seq(toTreeComponent(dt.rootNode)), dt.featureImportances.toArray)

  private def toTreeModel(dt: DecisionTreeClassificationModel): TreeModel =
    TreeModel("Decision Tree", Seq(toTreeComponent(dt.rootNode)), dt.featureImportances.toArray)

  private def toTreeModel(rf: RandomForestRegressionModel): TreeModel = TreeModel(
    "Random Forest",
    (rf.treeWeights zip rf.trees).map(p => toTreeComponent(p._2.rootNode, p._1)),
    rf.featureImportances.toArray
  )

  private def toTreeModel(rf: RandomForestClassificationModel): TreeModel = TreeModel(
    "Random Forest",
    (rf.treeWeights zip rf.trees).map(p => toTreeComponent(p._2.rootNode, p._1)),
    rf.featureImportances.toArray
  )

  private def toTreeModel(gbt: GBTRegressionModel): TreeModel = TreeModel(
    "GBT",
    (gbt.treeWeights zip gbt.trees).map(p => toTreeComponent(p._2.rootNode, p._1)),
    gbt.featureImportances.toArray
  )

  private def toTreeModel(gbt: GBTClassificationModel): TreeModel = TreeModel(
    "GBT",
    (gbt.treeWeights zip gbt.trees).map(p => toTreeComponent(p._2.rootNode, p._1)),
    gbt.featureImportances.toArray
  )

  // TODO: Rather than having the methods above, we could just overload the apply method.
  /**
    * Given a Spark tree model, create a Thrift TreeModel.
    * @param x - The Transformer.
    * @return The Thrift TreeModel associated with the Transformer. This is None if the
    *         Transformer is not a valid Spark tree model.
    */
  def apply(x: Transformer): Option[TreeModel] = x match {
    case m: DecisionTreeClassificationModel => Some(toTreeModel(m))
    case m: DecisionTreeRegressionModel => Some(toTreeModel(m))
    case m: RandomForestClassificationModel => Some(toTreeModel(m))
    case m: RandomForestRegressionModel => Some(toTreeModel(m))
    case m: GBTClassificationModel => Some(toTreeModel(m))
    case m: GBTRegressionModel => Some(toTreeModel(m))
    case _ => None
  }

}
