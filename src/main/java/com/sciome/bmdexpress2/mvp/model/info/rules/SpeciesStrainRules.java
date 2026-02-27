package com.sciome.bmdexpress2.mvp.model.info.rules;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.sciome.bmdexpress2.mvp.model.info.VocabularyConfig;

import java.util.*;

/**
 * Rules for Species and Strain dependencies.
 *
 * Dependencies:
 * - Species -> Strain (only matching strains for the species)
 */
public class SpeciesStrainRules
{

	public static List<String> getStrainsForSpecies(String species)
	{
		return VocabularyConfig.getInstance().getStrainsForSpecies(species);
	}

	public static Set<String> getAllSpecies()
	{
		return new HashSet<>(VocabularyConfig.getInstance().getSpecies());
	}

	@Rule(name = "StrainSpeciesConsistencyRule",
		  description = "Strain must be valid for the selected species",
		  priority = 1)
	public static class StrainSpeciesConsistency
	{

		@Condition
		public boolean when(@Fact("metadata") MetadataFacts facts)
		{
			if (!facts.hasValue(facts.getSpecies()) || !facts.hasValue(facts.getStrain()))
			{
				return false;
			}

			List<String> validStrains = getStrainsForSpecies(facts.getSpecies());
			if (validStrains.isEmpty())
			{
				// No strain restrictions for this species
				return false;
			}

			return !validStrains.contains(facts.getStrain());
		}

		@Action
		public void then(@Fact("metadata") MetadataFacts facts)
		{
			List<String> validStrains = getStrainsForSpecies(facts.getSpecies());
			facts.addError(String.format(
				"Strain '%s' is not valid for species '%s'. Valid strains: %s",
				facts.getStrain(), facts.getSpecies(), validStrains));
			facts.setValidOptions("strain", validStrains);
		}
	}
}
