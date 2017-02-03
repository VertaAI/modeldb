package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.AnnotationEvent

/**
  * This trait augments an object with an annotate function. Most of
  * the heavy lifting is done by AnnotationEvent in ModelDbSyncer.
  */
trait Annotater {

  /**
    * Create an annotation in ModelDB with the given items.
    *
    * @param items - The items that make up the annotation. They should be
    *              either Strings, Transformers, DataFrames, or PipelineStages.
    */
  def annotate(items: Any*)(implicit mdbs: Option[ModelDbSyncer]): Unit =
    mdbs.get.buffer(AnnotationEvent(items:_*))
}