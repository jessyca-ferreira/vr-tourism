import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'dart:math';

class LocationProvider with ChangeNotifier {
  // Static location (Recife center - near attractions)
  static const LatLng _staticLocation = LatLng(-8.0594, -34.8716);
  
  bool _isLoading = false;
  String? _errorMessage;
  bool _hasPermission = true; // Always true for static version

  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get hasPermission => _hasPermission;

  LatLng? get currentLatLng => _staticLocation;

  Future<void> requestLocationPermission() async {
    // No-op for static version
    _hasPermission = true;
    notifyListeners();
  }

  Future<void> getCurrentLocation() async {
    // No-op for static version
    _hasPermission = true;
    notifyListeners();
  }

  Future<void> refreshLocation() async {
    // No-op for static version
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  double calculateDistance(LatLng destination) {
    // Simple distance calculation using Haversine formula
    const double earthRadius = 6371000; // Earth's radius in meters
    
    double lat1 = _staticLocation.latitude * (pi / 180);
    double lat2 = destination.latitude * (pi / 180);
    double deltaLat = (destination.latitude - _staticLocation.latitude) * (pi / 180);
    double deltaLng = (destination.longitude - _staticLocation.longitude) * (pi / 180);

    double a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2);
    double c = 2 * atan2(sqrt(a), sqrt(1 - a));

    return earthRadius * c;
  }

  String formatDistance(double distanceInMeters) {
    if (distanceInMeters < 1000) {
      return '${distanceInMeters.round()}m';
    } else {
      double distanceInKm = distanceInMeters / 1000;
      return '${distanceInKm.toStringAsFixed(1)}km';
    }
  }
} 