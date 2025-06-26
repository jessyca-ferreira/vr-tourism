import 'package:google_maps_flutter/google_maps_flutter.dart';

class Attraction {
  final String id;
  final String name;
  final String description;
  final String history;
  final String importance;
  final String imageUrl;
  final String category;
  final double rating;
  final int visitDuration; // in minutes
  final double price; // entry fee
  final LatLng location;
  final List<String> tags;
  final String address;
  final String phoneNumber;
  final String website;
  final Map<String, String> openingHours;

  Attraction({
    required this.id,
    required this.name,
    required this.description,
    required this.history,
    required this.importance,
    required this.imageUrl,
    required this.category,
    required this.rating,
    required this.visitDuration,
    required this.price,
    required this.location,
    required this.tags,
    required this.address,
    required this.phoneNumber,
    required this.website,
    required this.openingHours,
  });

  factory Attraction.fromJson(Map<String, dynamic> json) {
    return Attraction(
      id: json['id'],
      name: json['name'],
      description: json['description'],
      history: json['history'],
      importance: json['importance'],
      imageUrl: json['imageUrl'],
      category: json['category'],
      rating: json['rating'].toDouble(),
      visitDuration: json['visitDuration'],
      price: json['price'].toDouble(),
      location: LatLng(json['location']['lat'], json['location']['lng']),
      tags: List<String>.from(json['tags']),
      address: json['address'],
      phoneNumber: json['phoneNumber'],
      website: json['website'],
      openingHours: Map<String, String>.from(json['openingHours']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'history': history,
      'importance': importance,
      'imageUrl': imageUrl,
      'category': category,
      'rating': rating,
      'visitDuration': visitDuration,
      'price': price,
      'location': {
        'lat': location.latitude,
        'lng': location.longitude,
      },
      'tags': tags,
      'address': address,
      'phoneNumber': phoneNumber,
      'website': website,
      'openingHours': openingHours,
    };
  }
} 