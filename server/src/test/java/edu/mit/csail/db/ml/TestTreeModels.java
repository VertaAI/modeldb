package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.TreeModelDao;
import modeldb.InvalidFieldException;
import modeldb.TreeComponent;
import modeldb.TreeLink;
import modeldb.TreeNode;
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
}
