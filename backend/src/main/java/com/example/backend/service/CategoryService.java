package com.example.backend.service;

import com.example.backend.dto.request.CreateCategoryRequest;
import com.example.backend.dto.request.UpdateCategoryRequest;
import com.example.backend.dto.response.CategoryResponse;
import com.example.backend.entity.Category;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CategoryRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(
            CategoryRepository categoryRepository
    ) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Create category
     */
    @Transactional
    public CategoryResponse createCategory(
            CreateCategoryRequest request
    ) {
        String name = normalizeName(request.getName());

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name cannot be blank."
            );
        }

        Category parent = getParentCategory(
                request.getParentId()
        );

        String slug = generateUniqueSlug(name, null);

        Category category = new Category();

        category.setName(name);
        category.setSlug(slug);
        category.setImageUrl(
                normalizeImageUrl(request.getImageUrl())
        );
        category.setParent(parent);

        Category savedCategory =
                categoryRepository.save(category);

        return new CategoryResponse(savedCategory);
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(
            UUID id
    ) {
        Category category = findCategoryById(id);

        return new CategoryResponse(category);
    }

    /**
     * Update category
     */
    @Transactional
    public CategoryResponse updateCategory(
            UUID id,
            UpdateCategoryRequest request
    ) {
        Category category = findCategoryById(id);

        String name = normalizeName(request.getName());

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name cannot be blank."
            );
        }

        Category parent = getParentCategory(
                request.getParentId()
        );

        validateParent(category, parent);

        /*
         * Slug is generated from category name.
         * Only regenerate when the name changes.
         */
        if (!category.getName().equals(name)) {

            String slug = generateUniqueSlug(
                    name,
                    category.getId()
            );

            category.setSlug(slug);
        }

        category.setName(name);
        category.setParent(parent);
        category.setImageUrl(
                normalizeImageUrl(request.getImageUrl())
        );

        Category updatedCategory =
                categoryRepository.save(category);

        return new CategoryResponse(updatedCategory);
    }

    /**
     * Delete category
     */
    @Transactional
    public void deleteCategory(UUID id) {

        Category category = findCategoryById(id);

        /*
         * Do not allow deleting a category
         * that still has child categories.
         */
        if (categoryRepository.existsByParentId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete category because it has child categories."
            );
        }

        try {
            categoryRepository.delete(category);
            categoryRepository.flush();

        } catch (DataIntegrityViolationException exception) {

            /*
             * The database FK prevents deleting a category
             * that is still referenced by products.
             */
            throw new IllegalArgumentException(
                    "Cannot delete category because it is being used by products."
            );
        }
    }

    /**
     * Find category or throw 404
     */
    private Category findCategoryById(UUID id) {

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found."
                        )
                );
    }

    /**
     * Get parent category.
     *
     * null parentId means root category.
     */
    private Category getParentCategory(
            String parentId
    ) {

        if (parentId == null ||
                parentId.isBlank()) {

            return null;
        }

        UUID parentUuid;

        try {
            parentUuid = UUID.fromString(parentId);

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid parent category ID."
            );
        }

        return findCategoryById(parentUuid);
    }

    /**
     * Validate parent relationship.
     *
     * Prevent:
     * 1. Category being its own parent.
     * 2. Circular parent-child relationships.
     */
    private void validateParent(
            Category category,
            Category newParent
    ) {

        if (newParent == null) {
            return;
        }

        if (category.getId().equals(newParent.getId())) {
            throw new IllegalArgumentException(
                    "A category cannot be its own parent."
            );
        }

        Category current = newParent;

        while (current != null) {

            if (current.getId().equals(category.getId())) {
                throw new IllegalArgumentException(
                        "Cannot create a circular category hierarchy."
                );
            }

            current = current.getParent();
        }
    }

    /**
     * Generate unique slug.
     *
     * Example:
     *
     * Điện thoại
     * -> dien-thoai
     *
     * If already exists:
     *
     * dien-thoai
     * dien-thoai-2
     * dien-thoai-3
     */
    private String generateUniqueSlug(
            String name,
            UUID currentCategoryId
    ) {

        String baseSlug = createSlug(name);

        String slug = baseSlug;
        int counter = 2;

        while (true) {

            boolean exists;

            if (currentCategoryId == null) {
                exists = categoryRepository.existsBySlug(slug);
            } else {
                exists =
                        categoryRepository.existsBySlugAndIdNot(
                                slug,
                                currentCategoryId
                        );
            }

            if (!exists) {
                return slug;
            }

            slug = baseSlug + "-" + counter;
            counter++;
        }
    }

    /**
     * Convert category name to slug.
     */
    private String createSlug(String value) {

        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        String withoutAccents = normalized
                .replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        String slug = withoutAccents
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");

        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name cannot be converted to a valid slug."
            );
        }

        return slug;
    }

    /**
     * Clean category name.
     */
    private String normalizeName(String name) {

        if (name == null) {
            return "";
        }

        return name.trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Empty image URL becomes null.
     */
    private String normalizeImageUrl(String imageUrl) {

        if (imageUrl == null ||
                imageUrl.isBlank()) {

            return null;
        }

        return imageUrl.trim();
    }
}