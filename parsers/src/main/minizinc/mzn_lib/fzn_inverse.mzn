%-----------------------------------------------------------------------------%
% Constrains two arrays of int variables, 'f' and 'invf', to represent
% inverse functions.  All the values in each array must be within the index
% set of the other array.
%-----------------------------------------------------------------------------%
predicate fzn_inverse(array[int] of var int: f, int: foff,
                          array[int] of var int: invf, int: invfoff);

predicate fzn_inverse(array[int] of var int: f, array[int] of var int: invf) =
    fzn_inverse(f, min(index_set(invf)), invf, min(index_set(f)));
