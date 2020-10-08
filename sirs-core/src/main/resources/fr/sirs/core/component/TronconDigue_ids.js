function(doc) {
  if(doc['@class']=='fr.sirs.core.model.TronconDigue') {
    var viewDocument = {};
    for (var k in doc){
      if(("_id" === k) || ("designation"=== k)  || ("libelle"=== k)  )
        viewDocument[k] = doc[k];
    }
    emit(doc._id, viewDocument)
   }
}
