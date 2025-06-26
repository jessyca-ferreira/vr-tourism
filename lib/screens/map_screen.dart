import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:tourist_guide_app/models/attraction.dart';
import 'package:tourist_guide_app/providers/location_provider.dart';
import 'package:tourist_guide_app/providers/attractions_provider.dart';
import 'package:tourist_guide_app/screens/attraction_detail_screen.dart';
import 'package:tourist_guide_app/widgets/attraction_info_card.dart';
import 'package:tourist_guide_app/utils/theme.dart';
import 'dart:math' as math;

class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  GoogleMapController? _mapController;
  Attraction? _selectedAttraction;

  @override
  void initState() {
    super.initState();
  }

  double _getMarkerColor(String category) {
    switch (category) {
      case 'Museu':
        return BitmapDescriptor.hueBlue;
      case 'Centro Cultural':
        return BitmapDescriptor.hueViolet;
      case 'Teatro':
        return BitmapDescriptor.hueOrange;
      case 'Centro de Artesanato':
        return BitmapDescriptor.hueGreen;
      case 'Marco':
        return BitmapDescriptor.hueRed;
      case 'Religioso':
        return BitmapDescriptor.hueYellow;
      default:
        return BitmapDescriptor.hueGreen;
    }
  }

  void _onMarkerTapped(Attraction attraction) {
    setState(() {
      _selectedAttraction = attraction;
    });
  }

  void _onMapCreated(GoogleMapController controller) {
    _mapController = controller;
    // Add a small delay to ensure the map is fully loaded before fitting attractions
    Future.delayed(const Duration(milliseconds: 500), () {
      _fitAllAttractions();
    });
  }

  void _fitAllAttractions() {
    final attractionsProvider = Provider.of<AttractionsProvider>(context, listen: false);
    if (attractionsProvider.attractions.isNotEmpty && _mapController != null) {
      final attractions = attractionsProvider.attractions;
      
      // Calculate bounds to include all attractions
      double minLat = attractions.first.location.latitude;
      double maxLat = attractions.first.location.latitude;
      double minLng = attractions.first.location.longitude;
      double maxLng = attractions.first.location.longitude;
      
      for (var attraction in attractions) {
        minLat = math.min(minLat, attraction.location.latitude);
        maxLat = math.max(maxLat, attraction.location.latitude);
        minLng = math.min(minLng, attraction.location.longitude);
        maxLng = math.max(maxLng, attraction.location.longitude);
      }
      
      // Add padding to the bounds
      const padding = 0.01; // About 1km padding
      final bounds = LatLngBounds(
        southwest: LatLng(minLat - padding, minLng - padding),
        northeast: LatLng(maxLat + padding, maxLng + padding),
      );
      
      try {
        _mapController!.animateCamera(
          CameraUpdate.newLatLngBounds(bounds, 50.0), // 50px padding
        );
      } catch (e) {
        // Fallback to center on Recife if bounds calculation fails
        _mapController!.animateCamera(
          CameraUpdate.newLatLngZoom(
            const LatLng(-8.0594, -34.8716), // Recife center
            14.0,
          ),
        );
      }
    }
  }

  void _animateToUserLocation() {
    final locationProvider =
        Provider.of<LocationProvider>(context, listen: false);
    if (locationProvider.currentLatLng != null && _mapController != null) {
      _mapController!.animateCamera(
        CameraUpdate.newLatLngZoom(
          locationProvider.currentLatLng!,
          14.0,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Consumer2<LocationProvider, AttractionsProvider>(
        builder: (context, locationProvider, attractionsProvider, child) {
          // Create markers directly here
          Set<Marker> markers = {};
          for (var attraction in attractionsProvider.attractions) {
            print('Creating marker for: ${attraction.name} at ${attraction.location}');
            final marker = Marker(
              markerId: MarkerId(attraction.id),
              position: attraction.location,
              infoWindow: InfoWindow(
                title: attraction.name,
                snippet: attraction.description,
                onTap: () => _onMarkerTapped(attraction),
              ),
              icon: BitmapDescriptor.defaultMarkerWithHue(
                _getMarkerColor(attraction.category),
              ),
              onTap: () => _onMarkerTapped(attraction),
            );
            markers.add(marker);
          }
          
          return Stack(
            children: [
              GoogleMap(
                onMapCreated: _onMapCreated,
                initialCameraPosition: const CameraPosition(
                  target: LatLng(-8.0594, -34.8716), // Centered on Museu Cais do Sertão area
                  zoom: 14.0, // Closer zoom to show attractions clearly
                ),
                markers: markers,
                myLocationEnabled: false, // Disabled for static version
                myLocationButtonEnabled: false,
                zoomControlsEnabled: false,
                mapToolbarEnabled: false,
                onTap: (_) {
                  setState(() {
                    _selectedAttraction = null;
                  });
                },
              ),

              // Floating action buttons
              Positioned(
                top: MediaQuery.of(context).padding.top + 16,
                right: 16,
                child: Column(
                  children: [
                    FloatingActionButton.small(
                      onPressed: _animateToUserLocation,
                      backgroundColor: AppTheme.surfaceColor,
                      child: const Icon(Icons.my_location,
                          color: AppTheme.primaryColor),
                    ),
                    const SizedBox(height: 8),
                    FloatingActionButton.small(
                      onPressed: _fitAllAttractions,
                      backgroundColor: AppTheme.surfaceColor,
                      child: const Icon(Icons.map,
                          color: AppTheme.primaryColor),
                    ),
                    const SizedBox(height: 8),
                    FloatingActionButton.small(
                      onPressed: () {
                        final locationProvider = Provider.of<LocationProvider>(
                            context,
                            listen: false);
                        locationProvider.refreshLocation();
                      },
                      backgroundColor: AppTheme.surfaceColor,
                      child: const Icon(Icons.refresh,
                          color: AppTheme.primaryColor),
                    ),
                  ],
                ),
              ),

              // Selected attraction info card
              if (_selectedAttraction != null)
                Positioned(
                  bottom: 16,
                  left: 16,
                  right: 16,
                  child: Container(
                    constraints: const BoxConstraints(
                      maxHeight: 400, // Limit maximum height for expanded cards
                    ),
                    child: SingleChildScrollView(
                      child: AttractionInfoCard(
                        attraction: _selectedAttraction!,
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => AttractionDetailScreen(
                                attraction: _selectedAttraction!,
                              ),
                            ),
                          );
                        },
                        onClose: () {
                          setState(() {
                            _selectedAttraction = null;
                          });
                        },
                      ),
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}
