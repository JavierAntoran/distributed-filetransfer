%%
file_size = rand(1,1)*1e10;
%
serverBW = [0.1 0.5 1 2 3 4 5 6 7 8 9 10] * 1e6;
servers = 1:length(serverBW);
chunksize = 1e6;
% fraction based distribution for whole chunks
totalBW = sum(serverBW);
relativeBW = serverBW ./ totalBW;
nchunks = ceil(file_size/chunksize);
bytesbyserv  = file_size .* relativeBW;
chunksbyserv_f = bytesbyserv / chunksize;
chunksbyserv = floor(chunksbyserv_f);
unasignedBytes = file_size - sum(chunksbyserv) * chunksize;
if unasignedBytes <= 0
    fprintf('exact result, run again please')
end
unasignedChunk = ceil(unasignedBytes / chunksize);
% purely fractional method
extra_space = chunksbyserv_f - chunksbyserv;
[sorted, index] = sort(extra_space, 'descend');
used_index = index(1:unasignedChunk);
chunksbyserv1 = chunksbyserv;
chunksbyserv1(used_index) = chunksbyserv(used_index) + 1;
chunksbyserv1(used_index(end)) = chunksbyserv1(used_index(end)) - 1 + mod(file_size, chunksize) / chunksize;
time1 = max(chunksbyserv1 * chunksize  ./ serverBW);
display1 = sprintf('method1: correct chunks: %d, time: %d\n', sum(chunksbyserv1) == file_size / chunksize, time1);
fprintf(display1)
%



%time based distribution
servertime = chunksize .* chunksbyserv ./ serverBW;
chunkvec = ones(1,unasignedChunk);
%
chunkvec(end) = mod(file_size, chunksize) / chunksize;
resultingtime = zeros(1, length(servers));
asignedch = sum(chunksbyserv);
%
for i = 1:unasignedChunk
   asignedch = chunkvec(i) + asignedch;
   addtime = chunksize .* chunkvec(i) ./  serverBW;
   
   resultingtime = servertime + addtime;
   [inc, chosen] = min(resultingtime);
   servertime(chosen) = inc;
end

time2 = max(servertime);
display2 = sprintf('method2: correct chunks: %d, time: %d\n\n', asignedch == file_size / chunksize, time2);
fprintf(display2)
