import os
import django
import json
from kafka import KafkaConsumer

# Set up Django environment
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "supplycrate.settings")
django.setup()

from supplycrate.documents import SearchIndexDocument
from supplycrate.views import redis_client


def start_kafka_consumer():

    def clear_related_caches(name: str, category_name: str, brand_name: str):
        name_pattern = f"*{name}*"

        category_pattern = f"*:{category_name}:*"
        brand_pattern = f"*:*:{brand_name}:*"

        # Combine patterns into search keys for Redis SCAN
        patterns = [
            f"search:{name_pattern}",
            f"search:{category_pattern}",
            f"search:{brand_pattern}",
        ]

        for pattern in patterns:
            for key in redis_client.scan_iter(match=pattern):
                print(key)
                redis_client.delete(key)

    consumer = KafkaConsumer(
        "product-service-events",
        bootstrap_servers=["kafka:29092"],
        auto_offset_reset="earliest",
        group_id="search_discovery_group",
        value_deserializer=lambda x: json.loads(x.decode("utf-8")),
    )

    print("Kafka consumer started, listening for events...")

    for message in consumer:
        try:

            event_type = message.key.decode("utf-8") if message.key else None
            event_data = message.value
            print("receive ccc")
            if event_type in ["ProductCreated", "ProductUpdated"]:
                print(f"Processing event: {event_type}")

                product_id = event_data.get("id")
                name = event_data.get("name", "")
                sku = event_data.get("sku", "")
                description = event_data.get("description", "")
                price = float(event_data.get("price", {}).get("amount", 0.0))
                currency = event_data.get("price", {}).get("currency", "USD")
                category = event_data.get("category", {})
                brand = event_data.get("brand", {})
                stock_qty = int(event_data.get("stockQty", 0))
                status = event_data.get("status", "ACTIVE")
                user_id = event_data.get("userId", "")

                doc = {
                    "id": f"prod_{product_id}",
                    "contentType": "PRODUCT",
                    "contentId": str(product_id),
                    "title": name,
                    "title_suggest": {"input": [name]},  # autocomplete field
                    "title_ngram": name,
                    "description": description,
                    "keywords": list(
                        filter(
                            None,
                            [
                                name.lower(),
                                sku.lower(),
                                brand.get("name", "").lower(),
                                category.get("name", "").lower(),
                            ],
                        )
                    ),
                    "metadata": {
                        "categoryId": str(category.get("id", "")),
                        "categoryName": category.get("name", "Unknown"),
                        "brandId": str(brand.get("id", "")),
                        "brandName": brand.get("name", "Unknown"),
                        "sku": sku,
                        "price": price,
                        "currency": currency,
                        "stockQty": stock_qty,
                        "status": status,
                        "userId": str(user_id),
                        "providerType": "STANDARD",
                    },
                }

                try:
                    SearchIndexDocument(meta={"id": doc["id"]}, **doc).save()

                    print(f"Indexed product: {event_data['name']}")
                except Exception as e:
                    print(f"Failed to index product: {e}")

            elif event_type == "ProductDeleted":
                print(f"Processing ProductDeleted event: {event_data}")
                product_id = event_data.get("id")
                doc_id = f"prod_{product_id}"
                try:
                    SearchIndexDocument.get(
                        id=doc_id,
                    ).delete()
                    print(f"Deleted product from index: {doc_id}")

                except Exception as e:

                    print(f"Failed to delete product: {e}")

            clear_related_caches(name, category.get("name", ""), brand.get("name", ""))
        except Exception as e:
            print(f"Error processing message: {e}")
    consumer.close()


if __name__ == "__main__":
    start_kafka_consumer()
