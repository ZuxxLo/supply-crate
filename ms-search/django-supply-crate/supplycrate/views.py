from typing import cast
from rest_framework.views import APIView
from rest_framework.response import Response
from elasticsearch_dsl import Search
from .documents import SearchIndexDocument
from rest_framework import status
import redis
import json


redis_client = redis.Redis(host="redis", port=6379, db=0)


class SearchView(APIView):
    def get(self, request):
        print(request)
        query = request.query_params.get("q", "").strip()
        category = request.query_params.get("category", "").strip()
        brand = request.query_params.get("brand", "").strip()

        max_price = request.query_params.get("maxPrice", "").strip()
        sort = request.query_params.get("sort", "relevance")

        # Check Redis cache
        cache_key = f"search:{query}:{category}:{brand}"
        cached = redis_client.get(cache_key)
        if cached:
            return Response({"results": json.loads(cached)}, status=status.HTTP_200_OK)

        # Build Elasticsearch query
        if query:
            s = Search(index="search_index").query(
                "multi_match",
                query=query,
                fields=["title^3", "description", "keywords"],
                fuzziness="AUTO",
            )
        else:
            s = Search(index="search_index").query("match_all")

        if category:
            s = s.filter("term", **{"metadata.categoryName": category})

        if brand:

            print(brand)
            s = s.filter("term", **{"metadata.brandName": brand})

        if max_price:
            s = s.filter("range", **{"metadata.price": {"lte": float(max_price)}})

        s = s.sort("metadata.price")  # by default
        if sort == "descending":
            s = s.sort("-metadata.price")

        try:
            print("ES Query:", json.dumps(s.to_dict(), indent=2))

            response = s.execute()
            print(response)
        except Exception as e:
            return Response(
                {"error": f"Search failed: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        results = [
            {
                "id": hit.meta.id,
                "contentType": hit.contentType,
                "title": hit.title,
                "sku": hit.metadata.sku,
                "description": hit.description,
                "price": hit.metadata.price,
                "category": hit.metadata.categoryName,
                "brand": hit.metadata.brandName,
            }
            for hit in response
        ]

        redis_client.setex(
            cache_key, 3600, json.dumps(results)
        )  # Cache for 1 hour so new docs when searching sth as the key above, wont be returned
        return Response({"results": results}, status=status.HTTP_200_OK)


class AutocompleteView(APIView):
    def get(self, request):
        query = request.query_params.get("q", "")
        cache_key = f"autocomplete:{query}"
        cached = redis_client.get(cache_key)

        if cached:
            return Response(
                {"suggestions": json.loads(cached)}, status=status.HTTP_200_OK
            )

        s = Search(index="search_index").suggest(
            "title_suggestion", query, completion={"field": "title_suggest"}
        )

        try:
            response = s.execute()
            suggestions = [
                suggestion.text
                for suggestion in response.suggest.title_suggestion[0].options
            ]
            redis_client.setex(cache_key, 3600, json.dumps(suggestions))
            return Response({"suggestions": suggestions}, status=status.HTTP_200_OK)
        except Exception as e:
            return Response(
                {"error": f"Autocomplete failed: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
