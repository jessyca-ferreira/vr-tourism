import 'package:flutter/material.dart';
import 'package:tourist_guide_app/models/attraction.dart';
import 'package:tourist_guide_app/utils/theme.dart';

class AttractionCard extends StatelessWidget {
  final Attraction attraction;
  final double distance;
  final VoidCallback onTap;

  const AttractionCard({
    super.key,
    required this.attraction,
    required this.distance,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Card(
        margin: const EdgeInsets.symmetric(vertical: 8),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        elevation: 2,
        child: Row(
          children: [
            ClipRRect(
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(16),
                bottomLeft: Radius.circular(16),
              ),
              child: Image.network(
                attraction.imageUrl,
                width: 100,
                height: 100,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) => Container(
                  width: 100,
                  height: 100,
                  color: AppTheme.textSecondaryColor.withOpacity(0.2),
                  child: const Icon(Icons.image, color: AppTheme.textSecondaryColor),
                ),
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      attraction.name,
                      style: AppTheme.heading3.copyWith(fontSize: 18),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      attraction.description,
                      style: AppTheme.body2,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(Icons.location_on, size: 16, color: AppTheme.primaryColor),
                        const SizedBox(width: 4),
                        Text(
                          distance < 1000
                              ? '${distance.round()}m'
                              : '${(distance / 1000).toStringAsFixed(1)}km',
                          style: AppTheme.caption,
                        ),
                        const SizedBox(width: 16),
                        Icon(Icons.star, size: 16, color: Colors.amber),
                        const SizedBox(width: 4),
                        Text(
                          attraction.rating.toString(),
                          style: AppTheme.caption,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
} 