package dr.evomodel.treedatalikelihood.continuous.cdi;

import dr.math.matrixAlgebra.WrappedVector;
import dr.math.matrixAlgebra.missingData.InversionResult;
import org.ejml.data.DenseMatrix64F;

import static dr.math.matrixAlgebra.missingData.MissingOps.*;

/**
 * @author Marc A. Suchard
 */

public class SafeMultivariateWithDriftIntegrator extends SafeMultivariateIntegrator {

    private static boolean DEBUG = false;

    public SafeMultivariateWithDriftIntegrator(PrecisionType precisionType, int numTraits, int dimTrait, int bufferCount,
                                               int diffusionCount) {
        super(precisionType, numTraits, dimTrait, bufferCount, diffusionCount);

        allocateStorage();

        System.err.println("Trying SafeMultivariateWithDriftIntegrator");
    }

    @Override
    public void getBranchDisplacement(int bufferIndex, double[] displacement) {

        if (bufferIndex == -1) {
            throw new RuntimeException("Not yet implemented");
        }

        assert (displacement != null);
        assert (displacement.length >= dimTrait);

        System.arraycopy(displacements, bufferIndex * dimTrait,
                displacement, 0, dimTrait);
    }

    @Override
    public void getBranchExpectation(double[] actualization, double[] parentValue, double[] displacement,
                                     double[] expectation) {

        assert (expectation != null);
        assert (expectation.length >= dimTrait);

        assert (parentValue != null);
        assert (parentValue.length >= dimTrait);

        assert (displacement != null);
        assert (displacement.length >= dimTrait);

        for (int i = 0; i < dimTrait; ++i) {
            expectation[i] = parentValue[i] + displacement[i];
        }
    }

    private static final boolean TIMING = false;

    private double[] vectorDispi;
    private double[] vectorDispj;

    private void allocateStorage() {

        displacements = new double[dimTrait * bufferCount];
        vectorDispi = new double[dimTrait];
        vectorDispj = new double[dimTrait];
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Setting variances, displacement and actualization vectors
    ///////////////////////////////////////////////////////////////////////////

    public void updateBrownianDiffusionMatrices(int precisionIndex, final int[] probabilityIndices,
                                                final double[] edgeLengths, final double[] driftRates,
                                                int updateCount) {

        super.updateBrownianDiffusionMatrices(precisionIndex, probabilityIndices, edgeLengths, driftRates, updateCount);

        if (DEBUG) {
            System.err.println("Matrices (safe with drift):");
        }

        if (driftRates != null) {

            assert (displacements != null);
            assert (driftRates.length >= updateCount * dimTrait);

            if (TIMING) {
                startTime("drift1");
            }

            int offset = 0;
            for (int up = 0; up < updateCount; ++up) {

                final double edgeLength = edgeLengths[up];
                final int pio = dimTrait * probabilityIndices[up];

                scale(driftRates, offset, edgeLength, displacements, pio, dimTrait);
                offset += dimTrait;
            }

            if (TIMING) {
                endTime("drift1");
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Tree-traversal functions
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void updatePreOrderPartial(
            final int kBuffer, // parent
            final int iBuffer, // node
            final int iMatrix,
            final int jBuffer, // sibling
            final int jMatrix) {

        throw new RuntimeException("Not yet implemented");
    }
//
//    @Override
//    protected void updatePartial(
//            final int kBuffer,
//            final int iBuffer,
//            final int iMatrix,
//            final int jBuffer,
//            final int jMatrix,
//            final boolean incrementOuterProducts
//    ) {
//
//        if (incrementOuterProducts) {
//            throw new RuntimeException("Outer-products are not supported.");
//        }
//
//        if (TIMING) {
//            startTime("total");
//        }
//
//        // Determine buffer offsets
//        int kbo = dimPartial * kBuffer;
//        int ibo = dimPartial * iBuffer;
//        int jbo = dimPartial * jBuffer;
//
//        // Determine matrix offsets
//        final int imo = dimTrait * dimTrait * iMatrix;
//        final int jmo = dimTrait * dimTrait * jMatrix;
//
//        // Read variance increments along descendent branches of k
//
//        // TODO Fix
////        final double vi = variances[imo];
////        final double vj = variances[jmo];
//
//        final DenseMatrix64F Vd = wrap(inverseDiffusions, precisionOffset, dimTrait, dimTrait);
////        final DenseMatrix64F Pd = wrap(diffusions, precisionOffset, dimTrait, dimTrait);
//
//        final DenseMatrix64F Vdi = wrap(variances, imo, dimTrait, dimTrait);
//        final DenseMatrix64F Vdj = wrap(variances, jmo, dimTrait, dimTrait);
//
//        final DenseMatrix64F Pdi = wrap(precisions, imo, dimTrait, dimTrait); // TODO Only if needed
//        final DenseMatrix64F Pdj = wrap(precisions, jmo, dimTrait, dimTrait); // TODO Only if needed
//
//        // TODO End fix
//
//        if (DEBUG) {
//            System.err.println("variance diffusion: " + Vd);
////            System.err.println("\tvi: " + vi + " vj: " + vj);
//            System.err.println("precisionOffset = " + precisionOffset);
//            System.err.println("\tVdi: " + Vdi);
//            System.err.println("\tVdj: " + Vdj);
//        }
//
//        // For each trait // TODO in parallel
//        for (int trait = 0; trait < numTraits; ++trait) {
//
//            // Layout, offset, dim
//            // trait, 0, dT
//            // precision, dT, dT * dT
//            // variance, dT + dT * dT, dT * dT
//            // scalar, dT + 2 * dT * dT, 1
//
//            if (TIMING) {
//                startTime("peel1");
//            }
//
//            // Increase variance along the branches i -> k and j -> k
//
//            // A. Get current precision of i and j
////            final double lpi = partials[ibo + dimTrait + 2 * dimTrait * dimTrait];
////            final double lpj = partials[jbo + dimTrait + 2 * dimTrait * dimTrait];
//
//            final DenseMatrix64F Pi = wrap(partials, ibo + dimTrait, dimTrait, dimTrait);
//            final DenseMatrix64F Pj = wrap(partials, jbo + dimTrait, dimTrait, dimTrait);
//
//            if (TIMING) {
//                endTime("peel1");
//                startTime("peel2");
//            }
//
//            // B. Integrate along branch using two matrix inversions
////            final double lpip = Double.isInfinite(lpi) ?
////                    1.0 / vi : lpi / (1.0 + lpi * vi);
////            final double lpjp = Double.isInfinite(lpj) ?
////                    1.0 / vj : lpj / (1.0 + lpj * vj);
//
//            InversionResult ci;
//            InversionResult cj;
//
//            final DenseMatrix64F Pip = matrix2;
//            final DenseMatrix64F Pjp = matrix3;
//
////            boolean useVariance = anyDiagonalInfinities(Pi) || anyDiagonalInfinities(Pj);
//            final boolean useVariancei = anyDiagonalInfinities(Pi);
//            final boolean useVariancej = anyDiagonalInfinities(Pj);
//
//            if (useVariancei) {
//
//                final DenseMatrix64F Vip = matrix0;
//                final DenseMatrix64F Vi = wrap(partials, ibo + dimTrait + dimTrait * dimTrait, dimTrait, dimTrait);
////                CommonOps.add(Vi, vi, Vd, Vip);  // TODO Fix
//                CommonOps.add(Vi, Vdi, Vip);
//                ci = safeInvert(Vip, Pip, true);
//
//            } else {
//
//                final DenseMatrix64F PiPlusPd = matrix0;
////                CommonOps.add(Pi, 1.0 / vi, Pd, PiPlusPd); // TODO Fix
//                CommonOps.add(Pi, Pdi, PiPlusPd);
//                final DenseMatrix64F PiPlusPdInv = new DenseMatrix64F(dimTrait, dimTrait);
//                safeInvert(PiPlusPd, PiPlusPdInv, false);
//                CommonOps.mult(PiPlusPdInv, Pi, Pip);
//                CommonOps.mult(Pi, Pip, PiPlusPdInv);
//                CommonOps.add(Pi, -1, PiPlusPdInv, Pip);
//                ci = safeDeterminant(Pip, false);
//            }
//
//            if (useVariancej) {
//
//                final DenseMatrix64F Vjp = matrix1;
//                final DenseMatrix64F Vj = wrap(partials, jbo + dimTrait + dimTrait * dimTrait, dimTrait, dimTrait);
////                CommonOps.add(Vj, vj, Vd, Vjp); // TODO Fix
//                CommonOps.add(Vj, Vdj, Vjp);
//                cj = safeInvert(Vjp, Pjp, true);
//
//            } else {
//
//                final DenseMatrix64F PjPlusPd = matrix1;
////                CommonOps.add(Pj, 1.0 / vj, Pd, PjPlusPd); // TODO Fix
//                CommonOps.add(Pj, Pdj, PjPlusPd);
//                final DenseMatrix64F PjPlusPdInv = new DenseMatrix64F(dimTrait, dimTrait);
//                safeInvert(PjPlusPd, PjPlusPdInv, false);
//                CommonOps.mult(PjPlusPdInv, Pj, Pjp);
//                CommonOps.mult(Pj, Pjp, PjPlusPdInv);
//                CommonOps.add(Pj, -1, PjPlusPdInv, Pjp);
//                cj = safeDeterminant(Pjp, false);
//            }
//
//            if (TIMING) {
//                endTime("peel2");
//                startTime("peel3");
//            }
//
//            // Compute partial mean and precision at node k
//
//            // A. Partial precision and variance (for later use) using one matrix inversion
////            final double lpk = lpip + lpjp;
//
////                final DenseMatrix64F Pk = new DenseMatrix64F(dimTrait, dimTrait);
//            final DenseMatrix64F Pk = matrix4;
//            CommonOps.add(Pip, Pjp, Pk);
//
////                final DenseMatrix64F Vk = new DenseMatrix64F(dimTrait, dimTrait);
////            final DenseMatrix64F Vk = matrix5;
//
////            if (useVariance) {
//////            InversionResult ck =
////                    safeInvert(Pk, Vk, true);
////            }
////            InversionResult ck = safeDeterminant(Pk, true);
//
////            System.err.println(ck);
////            System.err.println(ck2);
////            System.exit(-1);
//
//            if (TIMING) {
//                endTime("peel3");
//                startTime("peel4");
//            }
//
//            // B. Partial mean
//
//            final double[] displacementi = vector1;
//            final double[] displacementj = vector2;
//
//            final int ido = dimTrait * iMatrix;
//            final int jdo = dimTrait * jMatrix;
//
//            for (int g = 0; g < dimTrait; ++g) {
//                displacementi[g] = partials[ibo + g] - displacements[ido + g];
//                displacementj[g] = partials[jbo + g] - displacements[jdo + g];
//            }
//
////                final double[] tmp = new double[dimTrait];
//            final double[] tmp = vector0;
////            for (int g = 0; g < dimTrait; ++g) {
////                double sum = 0.0;
////                for (int h = 0; h < dimTrait; ++h) {
////                    sum += Pip.unsafe_get(g, h) * displacementi[h];
////                    sum += Pjp.unsafe_get(g, h) * displacementj[h];
////                }
////                tmp[g] = sum;
////            }
//            weightedSum(displacementi, 0, Pip, displacementj, 0, Pjp, dimTrait, tmp);
//
////            for (int g = 0; g < dimTrait; ++g) {
////                double sum = 0.0;
////                for (int h = 0; h < dimTrait; ++h) {
////                    sum += Vk.unsafe_get(g, h) * tmp[h];
////                }
////                partials[kbo + g] = sum;
////            }
//
//            final WrappedVector kPartials = new WrappedVector.Raw(partials, kbo, dimTrait);
//            final WrappedVector wrapTmp = new WrappedVector.Raw(tmp, 0, dimTrait);
//
////            System.err.println(kPartials);
////            System.err.println(ck.getDeterminant());
//
//            InversionResult ck = safeSolve(Pk, wrapTmp, kPartials, true);
//
////            System.err.println(kPartials);
////            System.err.println(ck.getDeterminant());
////            System.exit(-1);
//
//            if (TIMING) {
//                endTime("peel4");
//                startTime("peel5");
//            }
//
//            // C. Store precision
////            partials[kbo + dimTrait + 2 * dimTrait * dimTrait] = lpk;
//
//            unwrap(Pk, partials, kbo + dimTrait);
//
////            if (useVariance) {
////                unwrap(Vk, partials, kbo + dimTrait + dimTrait * dimTrait);
////            }
//
//            if (TIMING) {
//                endTime("peel5");
//            }
//
//            if (DEBUG) {
//                reportMeansAndPrecisions(trait, ibo, jbo, kbo, Pi, Pj, Pk);
////                System.err.println("\ttrait: " + trait);
////                System.err.println("Pi: " + Pi);
////                System.err.println("Pj: " + Pj);
////                System.err.println("Pk: " + Pk);
////                System.err.print("\t\tmean i:");
////                for (int e = 0; e < dimTrait; ++e) {
////                    System.err.print(" " + partials[ibo + e]);
////                }
//                System.err.print("\t\tdisp i:");
//                for (int e = 0; e < dimTrait; ++e) {
//                    System.err.print(" " + displacements[ido + e]);
//                }
//                System.err.println("");
////                System.err.print("\t\tmean j:");
////                for (int e = 0; e < dimTrait; ++e) {
////                    System.err.print(" " + partials[jbo + e]);
////                }
//                System.err.print("\t\tdisp j:");
//                for (int e = 0; e < dimTrait; ++e) {
//                    System.err.print(" " + displacements[jdo + e]);
//                }
////                System.err.println("");
////                System.err.print("\t\tmean k:");
////                for (int e = 0; e < dimTrait; ++e) {
////                    System.err.print(" " + partials[kbo + e]);
////                }
////                System.err.println("");
//            }
//
//            // Computer remainder at node k
//            double remainder = 0.0;
//
//            if (DEBUG) {
//                reportInversions(ci, cj, ck, Pip, Pjp);
////                System.err.println("i status: " + ci);
////                System.err.println("j status: " + cj);
////                System.err.println("k status: " + ck);
////                System.err.println("Pip: " + Pip);
//////                System.err.println("Vip: " + Vip);
////                System.err.println("Pjp: " + Pjp);
//////                System.err.println("Vjp: " + Vjp);
//            }
//
//            if (!(ci.getReturnCode() == NOT_OBSERVED || cj.getReturnCode() == NOT_OBSERVED)) {
//
//                if (TIMING) {
//                    startTime("remain");
//                }
//
//                // Inner products
//                double SSk = 0;
//                double SSj = 0;
//                double SSi = 0;
//
//                // vector-matrix-vector TODO in parallel
//                for (int g = 0; g < dimTrait; ++g) {
////                    final double ig = partials[ibo + g];
////                    final double jg = partials[jbo + g];
//                    final double ig = displacementi[g];
//                    final double jg = displacementj[g];
//                    final double kg = partials[kbo + g];
//
//                    for (int h = 0; h < dimTrait; ++h) {
////                        final double ih = partials[ibo + h];
////                        final double jh = partials[jbo + h];
//                        final double ih = displacementi[h];
//                        final double jh = displacementj[h];
//                        final double kh = partials[kbo + h];
//
//                        SSi += ig * Pip.unsafe_get(g, h) * ih;
//                        SSj += jg * Pjp.unsafe_get(g, h) * jh;
//                        SSk += kg * Pk .unsafe_get(g, h) * kh;
//                    }
//                }
//
////                    final DenseMatrix64F Vt = new DenseMatrix64F(dimTrait, dimTrait);
////                final DenseMatrix64F Vt = matrix6;
////                CommonOps.add(Vip, Vjp, Vt);
//
////                if (DEBUG) {
////                    System.err.println("Vt: " + Vt);
////                }
//
//                int dimensionChange = ci.getEffectiveDimension() + cj.getEffectiveDimension()
//                        - ck.getEffectiveDimension();
//
////                    System.err.println(ci.getDeterminant());
////                    System.err.println(CommonOps.det(Vip));
////
////                    System.err.println(cj.getDeterminant());
////                    System.err.println(CommonOps.det(Vjp));
////
////                    System.err.println(1.0 / ck.getDeterminant());
////                    System.err.println(CommonOps.det(Vk));
//
//                remainder += -dimensionChange * LOG_SQRT_2_PI - 0.5 *
////                            (Math.log(CommonOps.det(Vip)) + Math.log(CommonOps.det(Vjp)) - Math.log(CommonOps.det(Vk)))
//                        (Math.log(ci.getDeterminant()) + Math.log(cj.getDeterminant()) + Math.log(ck.getDeterminant()))
//                        - 0.5 * (SSi + SSj - SSk);
//
//                // TODO Can get SSi + SSj - SSk from inner product w.r.t Pt (see outer-products below)?
//
//                if (DEBUG) {
//                    System.err.println("\t\t\tSSi = " + (SSi));
//                    System.err.println("\t\t\tSSj = " + (SSj));
//                    System.err.println("\t\t\tSSk = " + (SSk));
//                    System.err.println("\t\t\tdeti = " + Math.log(ci.getDeterminant()));
//                    System.err.println("\t\t\tdetj = " + Math.log(ci.getDeterminant()));
//                    System.err.println("\t\t\tdetk = " + Math.log(ci.getDeterminant()));
//                    System.err.println("\t\tremainder: " + remainder);
////                        System.exit(-1);
//                }
//
//                if (TIMING) {
//                    endTime("remain");
//                }
//
//            } // End if remainder
//
//            // Accumulate remainder up tree and store
//
//            remainders[kBuffer * numTraits + trait] = remainder
//                    + remainders[iBuffer * numTraits + trait] + remainders[jBuffer * numTraits + trait];
//
//            // Get ready for next trait
//            kbo += dimPartialForTrait;
//            ibo += dimPartialForTrait;
//            jbo += dimPartialForTrait;
//
//        }
//
//        if (TIMING) {
//            endTime("total");
//        }
//    }

    @Override
    InversionResult partialMean(int ibo, int jbo, int kbo,
                                int ido, int jdo) {
        if (TIMING) {
            startTime("peel4");
        }

        final double[] displacementi = vectorDispi;
        final double[] displacementj = vectorDispj;

        for (int g = 0; g < dimTrait; ++g) {
            displacementi[g] = partials[ibo + g] - displacements[ido + g];
            displacementj[g] = partials[jbo + g] - displacements[jdo + g];
        }

        final double[] tmp = vector0;

        computeWeightedSum(displacementi, displacementj, dimTrait, tmp);

        final WrappedVector kPartials = new WrappedVector.Raw(partials, kbo, dimTrait);
        final WrappedVector wrapTmp = new WrappedVector.Raw(tmp, 0, dimTrait);

        InversionResult ck = safeSolve(matrixPk, wrapTmp, kPartials, true);

        if (TIMING) {
            endTime("peel4");
            startTime("peel5");
        }
        if (DEBUG) {
            System.err.print("\t\tdisp i:");
            for (int e = 0; e < dimTrait; ++e) {
                System.err.print(" " + displacements[ido + e]);
            }
            System.err.println("");
            System.err.print("\t\tdisp j:");
            for (int e = 0; e < dimTrait; ++e) {
                System.err.print(" " + displacements[jdo + e]);
            }
        }
        return ck;
    }

    @Override
    double computeSS(final int ibo,
                     final DenseMatrix64F Pip,
                     final int jbo,
                     final DenseMatrix64F Pjp,
                     final int kbo,
                     final DenseMatrix64F Pk,
                     final int dimTrait) {
        return weightedThreeInnerProduct(vectorDispi, 0, Pip,
                vectorDispj, 0, Pjp,
                partials, kbo, Pk,
                dimTrait);
    }

    void computeWeightedSum(final double[] ipartial,
                            final double[] jpartial,
                            final int dimTrait,
                            final double[] out) {
        weightedSum(ipartial, 0, matrixPip, jpartial, 0, matrixPjp, dimTrait, out);
    }

    double[] displacements;
}