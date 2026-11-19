from typing import Any, Dict, List


def adapt_legacy_format(data: Any, schema: Dict[str, Any]) -> Dict[str, List[Dict[str, Any]]]:
    doc_id_field = schema.get("doc_id_field")
    entities_field = schema.get("entities_field")

    result: Dict[str, List[Dict[str, Any]]] = {}

    if isinstance(data, list):
        for doc in data:
            if not isinstance(doc, dict) or doc_id_field not in doc:
                continue
            doc_id = str(doc[doc_id_field])
            entities = doc.get(entities_field, [])
            if isinstance(entities, list):
                result[doc_id] = entities
        return result

    if isinstance(data, dict):
        for doc_id, pred_doc in data.items():
            entities: List[Dict[str, Any]] = []
            if isinstance(pred_doc, list):
                if all(isinstance(item, dict) for item in pred_doc):
                    entities = pred_doc
            elif isinstance(pred_doc, dict):
                if "predicted_" + entities_field in pred_doc:
                    maybe = pred_doc.get("predicted_" + entities_field)
                    if isinstance(maybe, list):
                        entities = maybe
                elif entities_field in pred_doc:
                    maybe = pred_doc.get(entities_field)
                    if isinstance(maybe, list):
                        entities = maybe
                elif "prediction" in pred_doc and isinstance(pred_doc["prediction"], list):
                    entities = pred_doc["prediction"]
            result[str(doc_id)] = entities
        return result

    return result
