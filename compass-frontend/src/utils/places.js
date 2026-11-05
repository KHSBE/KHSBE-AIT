const legacyShape = (place) => ({
  place_id: place.id,
  name: place.displayName,
  formatted_address: place.formattedAddress,
  geometry: { location: place.location },
  rating: place.rating,
  user_ratings_total: place.userRatingCount,
  opening_hours: place.regularOpeningHours
    ? {
        weekday_text: place.regularOpeningHours.weekdayDescriptions || [],
        periods: place.regularOpeningHours.periods || [],
      }
    : undefined,
  reviews: (place.reviews || []).map((review) => ({
    author_name: review.authorAttribution?.displayName,
    author_url: review.authorAttribution?.uri,
    profile_photo_url: review.authorAttribution?.photoURI,
    rating: review.rating,
    text: review.text,
    language: review.originalLanguageCode,
    relative_time_description: review.relativePublishTimeDescription,
  })),
  website: place.websiteURI,
  formatted_phone_number: place.nationalPhoneNumber,
  url: place.googleMapsURI,
  business_status: place.businessStatus,
  types: place.types || [],
});

export async function searchPlacesNew(query, options = {}) {
  const { Place } = await window.google.maps.importLibrary("places");
  const { places } = await Place.searchByText({
    textQuery: query,
    fields: ["id", "displayName", "formattedAddress", "location", "types"],
    language: "ko",
    ...options,
  });
  return (places || []).map(legacyShape);
}

export async function placeDetailNew(placeId) {
  const { Place } = await window.google.maps.importLibrary("places");
  const place = new Place({ id: placeId, requestedLanguage: "ko" });
  await place.fetchFields({
    fields: [
      "id",
      "displayName",
      "formattedAddress",
      "location",
      "rating",
      "userRatingCount",
      "regularOpeningHours",
      "reviews",
      "websiteURI",
      "nationalPhoneNumber",
      "googleMapsURI",
      "businessStatus",
    ],
  });
  return legacyShape(place);
}
