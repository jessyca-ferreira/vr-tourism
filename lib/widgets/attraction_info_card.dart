import 'package:flutter/material.dart';
import 'package:tourist_guide_app/models/attraction.dart';
import 'package:tourist_guide_app/utils/theme.dart';

class AttractionInfoCard extends StatefulWidget {
  final Attraction attraction;
  final VoidCallback onTap;
  final VoidCallback onClose;

  const AttractionInfoCard({
    super.key,
    required this.attraction,
    required this.onTap,
    required this.onClose,
  });

  @override
  State<AttractionInfoCard> createState() => _AttractionInfoCardState();
}

class _AttractionInfoCardState extends State<AttractionInfoCard> {
  bool _isExpanded = false;

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 6,
      borderRadius: BorderRadius.circular(16),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header row with image and basic info
            Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: Image.network(
                    widget.attraction.imageUrl,
                    width: 64,
                    height: 64,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => Container(
                      width: 64,
                      height: 64,
                      color: AppTheme.textSecondaryColor.withOpacity(0.2),
                      child: const Icon(Icons.image, color: AppTheme.textSecondaryColor),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.attraction.name,
                        style: AppTheme.heading3.copyWith(fontSize: 16),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        widget.attraction.description,
                        style: AppTheme.body2,
                        maxLines: _isExpanded ? null : 2,
                        overflow: _isExpanded ? null : TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Icon(Icons.star, size: 14, color: Colors.amber),
                          const SizedBox(width: 4),
                          Text(
                            widget.attraction.rating.toString(),
                            style: AppTheme.caption,
                          ),
                          const SizedBox(width: 16),
                          Icon(Icons.category, size: 14, color: AppTheme.primaryColor),
                          const SizedBox(width: 4),
                          Text(
                            widget.attraction.category,
                            style: AppTheme.caption.copyWith(
                              color: AppTheme.primaryColor,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                Column(
                  children: [
                    IconButton(
                      icon: Icon(
                        _isExpanded ? Icons.expand_less : Icons.expand_more,
                        color: AppTheme.textSecondaryColor,
                      ),
                      onPressed: () {
                        setState(() {
                          _isExpanded = !_isExpanded;
                        });
                      },
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, color: AppTheme.textSecondaryColor),
                      onPressed: widget.onClose,
                    ),
                  ],
                ),
              ],
            ),
            
            // Expanded content
            if (_isExpanded) ...[
              const SizedBox(height: 16),
              const Divider(),
              const SizedBox(height: 12),
              
              // Quick info row
              Row(
                children: [
                  Expanded(
                    child: _buildExpandedInfoItem(
                      icon: Icons.access_time,
                      label: 'Duração',
                      value: '${widget.attraction.visitDuration} min',
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: _buildExpandedInfoItem(
                      icon: Icons.euro,
                      label: 'Preço',
                      value: widget.attraction.price == 0 ? 'Grátis' : 'R\$${widget.attraction.price}',
                    ),
                  ),
                ],
              ),
              
              const SizedBox(height: 16),
              
              // Tags
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: widget.attraction.tags.map((tag) {
                  return Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryColor.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: AppTheme.primaryColor.withOpacity(0.3),
                      ),
                    ),
                    child: Text(
                      tag,
                      style: TextStyle(
                        color: AppTheme.primaryColor,
                        fontSize: 10,
                        fontWeight: FontWeight.w500,
                        fontFamily: 'Poppins',
                      ),
                    ),
                  );
                }).toList(),
              ),
              
              const SizedBox(height: 16),
              
              // Action buttons
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: widget.onTap,
                      icon: const Icon(Icons.info_outline, size: 16),
                      label: const Text('Ver Detalhes'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppTheme.primaryColor,
                        side: BorderSide(color: AppTheme.primaryColor),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: () {
                        // Get directions functionality
                      },
                      icon: const Icon(Icons.directions, size: 16),
                      label: const Text('Como Chegar'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryColor,
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildExpandedInfoItem({
    required IconData icon,
    required String label,
    required String value,
  }) {
    return Row(
      children: [
        Icon(
          icon,
          size: 16,
          color: AppTheme.primaryColor,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: AppTheme.caption.copyWith(
                  fontWeight: FontWeight.w500,
                ),
              ),
              Text(
                value,
                style: AppTheme.body2.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
} 