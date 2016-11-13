package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.TreeModelDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TreelinkRecord;
import jooq.sqlite.gen.tables.records.TreemodelRecord;
import jooq.sqlite.gen.tables.records.TreemodelcomponentRecord;
import jooq.sqlite.gen.tables.records.TreenodeRecord;
import modeldb.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class TestTreeModels {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testTreeCreation() throws Exception {
    // Create the following tree:
    //                    root
    //        left                      right
    // leftLeft   leftRight    rightLeft
    TreeNode root = new TreeNode(1.0, 0.4);
    int ROOT_INDEX = 1;

    TreeNode left = new TreeNode(1.1, 0.5);
    int LEFT_INDEX = 0;

    TreeNode right = new TreeNode(1.2, 0.6);
    int RIGHT_INDEX = 3;

    TreeNode leftLeft = new TreeNode(1.3, 0.7);
    int LEFT_LEFT_INDEX = 5;

    TreeNode leftRight = new TreeNode(1.4, 0.8);
    int LEFT_RIGHT_INDEX = 2;

    TreeNode rightLeft = new TreeNode(1.5, 0.9);
    int RIGHT_LEFT_INDEX = 4;

    List<TreeNode> nodes = new ArrayList<>();
    IntStream.range(0, 6).forEach(i -> nodes.add(null));
    nodes.set(ROOT_INDEX, root);
    nodes.set(LEFT_INDEX, left);
    nodes.set(RIGHT_INDEX, right);
    nodes.set(LEFT_LEFT_INDEX, leftLeft);
    nodes.set(LEFT_RIGHT_INDEX, leftRight);
    nodes.set(RIGHT_LEFT_INDEX, rightLeft);

    List<TreeLink> links = Arrays.asList(
      new TreeLink(ROOT_INDEX, LEFT_INDEX, true),
      new TreeLink(ROOT_INDEX, RIGHT_INDEX, false),
      new TreeLink(LEFT_INDEX, LEFT_LEFT_INDEX, true),
      new TreeLink(LEFT_INDEX, LEFT_RIGHT_INDEX, false),
      new TreeLink(RIGHT_INDEX, RIGHT_LEFT_INDEX, true)
    );

    TreeComponent component = new TreeComponent(1.0, nodes, links);

    TreeModelDao.Tree tree = new TreeModelDao.Tree(component);

    // Verify the hasLeftChild/hasRightChild methods.
    List<Integer> rightChilders = Arrays.asList(ROOT_INDEX, LEFT_INDEX);
    List<Integer> leftChilders = Arrays.asList(ROOT_INDEX, LEFT_INDEX, RIGHT_INDEX);

    leftChilders.forEach(i -> Assert.assertTrue(tree.hasLeftChild(i)));
    rightChilders.forEach(i -> Assert.assertTrue(tree.hasRightChild(i)));
    IntStream
      .range(0, nodes.size())
      .filter(i -> !(leftChilders.contains(i) || rightChilders.contains(i)))
      .forEach(i -> Assert.assertFalse(tree.hasLeftChild(i) || tree.hasRightChild(i)));

    // Verify the leftChild/rightChild methods.
    links.forEach(link -> {
      if (link.isLeft) {
        Assert.assertEquals(link.childIndex, tree.leftChild(link.parentIndex));
      } else {
        Assert.assertEquals(link.childIndex, tree.rightChild(link.parentIndex));
      }
    });

    // Verify the hasParent method.
    IntStream.range(0, nodes.size()).forEach(i -> Assert.assertEquals(i != ROOT_INDEX, tree.hasParent(i)));

    // Verify the parent method.
    links.forEach(link -> Assert.assertEquals(link.parentIndex, tree.parent(link.childIndex)));

    // Verify the isLeaf method.
    IntStream.range(0, nodes.size()).forEach(i ->
      Assert.assertEquals(i == LEFT_LEFT_INDEX || i == LEFT_RIGHT_INDEX || i == RIGHT_LEFT_INDEX, tree.isLeaf(i))
    );

    // Verify the get method.
    IntStream.range(0, nodes.size()).forEach(i -> Assert.assertEquals(nodes.get(i), tree.get(i)));
  }

  @Test
  public void testTreeCreationTwoNodes() throws Exception {
    TreeComponent component = new TreeComponent(
      1.0,
      Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6)),
      Collections.singletonList(new TreeLink(0, 1, true))
    );

    TreeModelDao.Tree tree = new TreeModelDao.Tree(component);
    Assert.assertEquals(1, tree.leftChild(0));
    Assert.assertEquals(0, tree.parent(1));
  }

  @Test
  public void testTreeCreationInvalidIndex() throws Exception {
    try {
      TreeComponent component = new TreeComponent(
        1.0,
        Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6)),
        Collections.singletonList(new TreeLink(0, 10, true))
      );

      new TreeModelDao.Tree(component);
      Assert.fail();
    } catch (InvalidFieldException ivf) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testTreeCreationInconsistentParent() throws Exception {
    try {
      TreeComponent component = new TreeComponent(
        1.0,
        Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6), new TreeNode(1.3, 0.6)),
        Arrays.asList(new TreeLink(0, 2, true), new TreeLink(1, 2, true))
      );

      new TreeModelDao.Tree(component);
      Assert.fail();
    } catch (InvalidFieldException ivf) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testTreeCreationInconsistentChild() throws Exception {
    try {
      TreeComponent component = new TreeComponent(
        1.0,
        Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6), new TreeNode(1.3, 0.6)),
        Arrays.asList(new TreeLink(0, 1, true), new TreeLink(0, 2, true))
      );

      new TreeModelDao.Tree(component);
      Assert.fail();
    } catch (InvalidFieldException ivf) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testTreeCreationTwoRoots() throws Exception {
    try {
      TreeComponent component = new TreeComponent(
        1.0,
        Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6)),
        Collections.emptyList()
      );

      new TreeModelDao.Tree(component);
      Assert.fail();
    } catch (InvalidFieldException ivf) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testTreeCreationNoRoot() throws Exception {
    try {
      TreeComponent component = new TreeComponent(
        1.0,
        Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6)),
        Arrays.asList(new TreeLink(0, 1, true), new TreeLink(1, 0, true))
      );

      new TreeModelDao.Tree(component);
      Assert.fail();
    } catch (InvalidFieldException ivf) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testStoreNode() throws Exception {
    TreeModelDao.storeNode(new TreeNode(1.0, 1.5), null, null, false, TestBase.ctx());

    // Verify table sizes.
    Assert.assertEquals(1, TestBase.tableSize(Tables.TREENODE));
    Assert.assertEquals(0, TestBase.tableSize(Tables.TREELINK));

    // Verify the node.
    TreenodeRecord node = TestBase.ctx().selectFrom(Tables.TREENODE).fetchOne();
    Assert.assertEquals(0, node.getIsleaf().intValue());
    Assert.assertEquals(1.0, node.getPrediction(), 0.01);
    Assert.assertEquals(1.5, node.getImpurity(), 0.01);
  }

  @Test
  public void testStoreComponent() throws Exception {
    int modelId = TestBase.createTransformer(triple.expRunId, "ttype", "");

    TreeModelDao.storeComponent(
      modelId,
      1,
      new TreeComponent(
        1.5,
        Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6)),
        Collections.singletonList(new TreeLink(0, 1, true))
      ),
      TestBase.ctx()
    );

    // Verify table sizes.
    Assert.assertEquals(2, TestBase.tableSize(Tables.TREENODE));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TREELINK));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TREEMODELCOMPONENT));

    // Verify the TreeNodes.
    List<TreenodeRecord> nodes = TestBase.ctx()
      .selectFrom(Tables.TREENODE)
      .orderBy(Tables.TREENODE.PREDICTION.asc())
      .fetch();
    Assert.assertEquals(0, nodes.get(0).getIsleaf().intValue());
    Assert.assertEquals(1.0, nodes.get(0).getPrediction(), 0.01);
    Assert.assertEquals(0.5, nodes.get(0).getImpurity(), 0.01);

    Assert.assertEquals(1, nodes.get(1).getIsleaf().intValue());
    Assert.assertEquals(1.1, nodes.get(1).getPrediction(), 0.01);
    Assert.assertEquals(0.6, nodes.get(1).getImpurity(), 0.01);

    // Verify the TreeLinks.
    TreelinkRecord link = TestBase.ctx().selectFrom(Tables.TREELINK).fetchOne();
    Assert.assertEquals(nodes.get(0).getId().intValue(), link.getParent().intValue());
    Assert.assertEquals(nodes.get(1).getId().intValue(), link.getChild().intValue());
    Assert.assertEquals(1, link.getIsleft().intValue());

    // Verify the TreeModelComponent.
    TreemodelcomponentRecord compRec = TestBase.ctx().selectFrom(Tables.TREEMODELCOMPONENT).fetchOne();
    Assert.assertEquals(modelId, compRec.getModel().intValue());
    Assert.assertEquals(1, compRec.getComponentindex().intValue());
    Assert.assertEquals(1.5, compRec.getComponentweight(), 0.01);
    Assert.assertEquals(nodes.get(0).getId().intValue(), compRec.getRootnode().intValue());
  }

  @Test
  public void testStore() throws Exception {
    int modelId = TestBase.createTransformer(triple.expRunId, "ttype", "");

    TreeModelDao.store(
      modelId,
      new TreeModel(
        "Random Forest",
        Arrays.asList(
          new TreeComponent(
            1.2,
            Arrays.asList(new TreeNode(1.0, 0.5), new TreeNode(1.1, 0.6)),
            Collections.singletonList(new TreeLink(0, 1, true))
          ),
          new TreeComponent(
            1.5,
            Arrays.asList(new TreeNode(1.3, 0.7), new TreeNode(1.4, 0.8)),
            Collections.singletonList(new TreeLink(0, 1, true))
          )
        ),
        Collections.emptyList()
      ),
      TestBase.ctx()
    );

    // Verify table sizes.
    Assert.assertEquals(4, TestBase.tableSize(Tables.TREENODE));
    Assert.assertEquals(2, TestBase.tableSize(Tables.TREELINK));
    Assert.assertEquals(2, TestBase.tableSize(Tables.TREEMODELCOMPONENT));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TREEMODEL));

    // Verify the TreeModel.
    TreemodelRecord rec = TestBase.ctx().selectFrom(Tables.TREEMODEL).fetchOne();
    Assert.assertEquals(modelId, rec.getModel().intValue());
    Assert.assertEquals("Random Forest", rec.getModeltype());
  }
}
