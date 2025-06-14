import os
from django.http import JsonResponse
from django.utils.deprecation import MiddlewareMixin

class GatewayOnlyMiddleware(MiddlewareMixin):
    def process_request(self, request):
        expected = os.getenv('GATEWAY_SECRET_KEY', '')
        header = request.headers.get('X-GATEWAY-AUTHORIZED', '')
        if header != expected:
            return JsonResponse(
                {"message": "Forbidden: Direct access not allowed."},
                status=403
            )
