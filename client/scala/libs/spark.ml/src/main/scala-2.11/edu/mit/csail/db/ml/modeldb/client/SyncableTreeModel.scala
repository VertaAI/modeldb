package edu.mit.csail.db.ml.modeldb.client

import modeldb.{TreeComponent, TreeLink, TreeModel, TreeNode}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, GBTClassificationModel, RandomForestClassificationModel}
import org.apache.spark.ml.regression.{DecisionTreeRegressionModel, GBTRegressionModel, RandomForestRegressionModel}
import org.apache.spark.ml.tree.{InternalNode, LeafNode, Node}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object SyncableTreeModel {
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

  private def toTreeComponent(rootNode: Node, weight: Double = 1.0): TreeComponent = {
    val indexForNode = mutable.HashMap[TreeNode, Int]()
    val nodes = ArrayBuffer[TreeNode]()
    val links = ArrayBuffer[TreeLink]()

    updateState(rootNode, nodes, links, indexForNode, None)
    TreeComponent(weight, nodes, links)
  }

  private def toTreeModel(dt: DecisionTreeRegressionModel): TreeModel =
    TreeModel("Decision Tree", Seq(toTreeComponent(dt.rootNode)))

  private def toTreeModel(dt: DecisionTreeClassificationModel): TreeModel =
    TreeModel("Decision Tree", Seq(toTreeComponent(dt.rootNode)))

  private def toTreeModel(rf: RandomForestRegressionModel): TreeModel = TreeModel(
    "Random Forest",
    (rf.treeWeights zip rf.trees).map(p => toTreeComponent(p._2.rootNode, p._1))
  )

  private def toTreeModel(rf: RandomForestClassificationModel): TreeModel = TreeModel(
    "Random Forest",
    (rf.treeWeights zip rf.trees).map(p => toTreeComponent(p._2.rootNode, p._1))
  )

  private def toTreeModel(gbt: GBTRegressionModel): TreeModel = TreeModel(
    "GBT",
    (gbt.treeWeights zip gbt.trees).map(p => toTreeComponent(p._2.rootNode, p._1))
  )

  private def toTreeModel(gbt: GBTClassificationModel): TreeModel = TreeModel(
    "GBT",
    (gbt.treeWeights zip gbt.trees).map(p => toTreeComponent(p._2.rootNode, p._1))
  )

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
