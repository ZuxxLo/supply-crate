from django_elasticsearch_dsl import Document, fields
from django_elasticsearch_dsl.registries import registry

from supplycrate.models import Product


@registry.register_document
class SearchIndexDocument(Document):
    contentType = fields.KeywordField()
    contentId = fields.KeywordField()
    title = fields.TextField(analyzer="standard")
    title_suggest = fields.CompletionField()  # For autocomplete
    title_ngram = fields.TextField(
        analyzer="edge_ngram_analyzer"
    )  # For partial matching
    description = fields.TextField(analyzer="standard")
    keywords = fields.TextField(multi=True)
    metadata = fields.ObjectField(
        properties={
            "categoryId": fields.KeywordField(),
            "categoryName": fields.KeywordField(),
            "brandId": fields.KeywordField(),
            "brandName": fields.KeywordField(),
            "price": fields.FloatField(),
            "currency": fields.KeywordField(),
            "stockQty": fields.IntegerField(),
            "status": fields.KeywordField(),
        }
    )

    class Index:
        name = "search_index"
        settings = {
            "number_of_shards": 1,
            "number_of_replicas": 1,
            "analysis": {
                "analyzer": {
                    "edge_ngram_analyzer": {
                        "type": "custom",
                        "tokenizer": "edge_ngram_tokenizer",
                        "filter": ["lowercase"],
                    }
                },
                "tokenizer": {
                    "edge_ngram_tokenizer": {
                        "type": "edge_ngram",
                        "min_gram": 2,
                        "max_gram": 10,
                        "token_chars": ["letter", "digit"],
                    }
                },
            },
        }

    class Django:
        model = Product  # Replace with your actual Django model

        related_models = []  # Add related models if needed (e.g., Category, Brand)

    def prepare_title_suggest(self, instance):
        return {"input": [instance.title], "weight": 10}

    def prepare_title_ngram(self, instance):
        return instance.title
