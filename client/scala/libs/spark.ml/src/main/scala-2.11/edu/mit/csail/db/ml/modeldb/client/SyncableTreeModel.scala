package edu.mit.csail.db.ml.modeldb.client

import modeldb.{TreeComponent, TreeLink, TreeModel, TreeNode}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.classification.DecisionTreeClassificationModel

object SyncableTreeModel {
  private def toTreeModel(dt: DecisionTreeClassificationModel): TreeModel = {
    val nodes = Seq[TreeNode]()
    val links = Seq[TreeLink]()

    TreeModel("Decision Tree", Seq(TreeComponent(1.0, nodes, links)))
  }

  def apply(x: Transformer): Option[TreeModel] = x match {
    case _ => None
  }
}
